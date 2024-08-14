node=$1
basedir=$2
clients=$3
ID=$4
duration=$5
locality=$6
warehouse=$7
msgs=$8
log=$9
clispernode=${10}
payload=${11}
thinktime=${12}
localm=${13}

for i in $(seq 1 $clispernode)
do
    java -cp "bin/*:lib/*" MainClient -c $clients -i $ID -d $duration  $log -np 4 >> $basedir/logs/client$ID.txt &
    sleep .1
    ID=$(($ID+1));
done