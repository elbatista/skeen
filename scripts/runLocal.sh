if [ "$#" -lt 7 ]; then 
    echo "Usage: $0 <duration:sec> <algo:0-flex;1-skeen;2-byz> <#clis> <#servers> <%locality> <#msgs> <#exp>"; 
    exit 0; 
fi

pkill -f 'java.*Main*'; 
ant clean; 
ant; 
i=0;
exe=0; 
log="";
duration=$1; 
algo=$2; 
clis=$3; 
servers=$4; 
locality=$5; 
msgs=$6; 
numExperiments=$7;

rm -f -r logs/* files/*; 
echo false > files/stop; 

for exe in $(seq 1 $numExperiments); do

    rm -f -r logs/*.txt files/* results/*; echo false > files/stop;
    echo "------------------------------------------------------------------------------------------------" >> logs/executions.log
    echo "execution $exe (of $numExperiments) at" $(date) >> logs/executions.log
    echo $0 duration: $duration\; algo: $algo\; clis: $clis\; servers: $servers\; locality: $locality%\; msgs: $msgs\; >> logs/executions.log
    echo "------------------------------------------------------------------------------------------------" >> logs/executions.log
    
    totalClis=$clis
    cliIds=0

    # Start servers
    ((START = $servers-1))
    for ((i = START; i >= 0; i-=1)) ; do
        java -cp "bin/*:lib/*" MainServer -i $i -a $algo -d $duration -c $totalClis $log >> logs/node$i.txt & sleep .05
    done
    echo started $servers servers >> logs/executions.log

    # Start clients
    ((END = $clis-1))
    warehouse=0
    for j in $(seq 0 $END); do
        if [ $warehouse -eq $servers ]; then warehouse=0; fi
        java -cp "bin/*:lib/*" MainClient -c $totalClis -i $cliIds -d $duration -a $algo -l $locality -w $warehouse -m $msgs $log  -payload >> logs/cli$j.txt &
        ((warehouse=$warehouse+1))
        ((cliIds = $cliIds+1))
    done
    echo started $clis clients >> logs/executions.log

    echo "waiting..."  >> logs/executions.log;
    while :
    do
        sleep 1;
        nodeFiles=`find ./files -name 'NodeFinished*' | wc -l` #Count files and store in a variable
        if [ "$nodeFiles" -ge $servers ]; then sleep 1; break; fi
        if grep -q "true" files/stop; then 
            echo "found stop, exiting..." >> logs/executions.log; 
            exit 0; 
        fi
    done
    echo "all nodes done"  >> logs/executions.log; 
    pkill -f 'java.*Main*'; 
    echo "processes killed"  >> logs/executions.log

done