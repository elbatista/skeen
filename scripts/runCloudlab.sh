#!/bin/bash

# run example:
# ./scripts/runCloudlab.sh 10 150 3 4 95 0 50 false false true
if [ "$#" -lt 10 ]; then 
    echo  "Usage: $0 <duration:sec> <#clis> <#servers> <#nodes> <locality> <#msgs> <#clispernode> <payload> <thinktime> <localm>"
    exit 0; 
fi

i=0;
ID=-1;
log="any"; # either -log or any
warehouse=0;
iniport=3000;
basedir=/usr/batista/skeen;
duration=$1;
clients=$2;
servers=$3;
nodes=$4;
locality=$5;
msgs=$6;
clispernode=$7;
payload=$8
thinktime=$9
localm=${10}

rm -f -r $basedir/logs $basedir/files $basedir/results;
mkdir $basedir/logs; mkdir $basedir/files; mkdir $basedir/results;

echo false > $basedir/files/stop;
echo "------------------------------------------------------------------------------------------------" >> $basedir/logs/execution.log;
echo "started experiment on" $(date) >> $basedir/logs/execution.log;
echo "duration=$1 clients=$clients servers=$servers nodes=$nodes locality=$locality msgs=$msgs \
clispernode=$clispernode  payload=$payload thinktime=$thinktime" >> $basedir/logs/execution.log;
echo "------------------------------------------------------------------------------------------------" >> $basedir/logs/execution.log;

./scripts/killAll.sh $nodes 1 >> $basedir/logs/execution.log;

# compile, create config file, and update all other nodes
echo creating hosts.config for $servers servers >> $basedir/logs/execution.log;
echo "#id ip port" > $basedir/config/hosts.config;

confserverid=0;
serverfile=$basedir/config/servers.conf
while IFS=, read -r node ip
do
    echo "$confserverid $ip $iniport" >> $basedir/config/hosts.config;
    confserverid=$(($confserverid+1));
    iniport=$(($iniport+10));
done < <( awk '!/^ *#/ && NF' "$serverfile");

cd $basedir;
echo compiling source code >> $basedir/logs/execution.log;
ant clean; ant;
echo updating all other nodes with source code, config, scripts, and directories >> $basedir/logs/execution.log;
for i in $(seq 1 $nodes)
do
    ssh -o StrictHostKeyChecking=accept-new node$i "rm -rf $basedir/*; mkdir -p $basedir/logs; mkdir -p $basedir/files; mkdir -p $basedir/results; mkdir -p $basedir/config;"
    scp -q -r -o StrictHostKeyChecking=accept-new $basedir/bin node$i:$basedir/bin
    scp -q -r -o StrictHostKeyChecking=accept-new $basedir/lib node$i:$basedir/lib
    scp -q -r -o StrictHostKeyChecking=accept-new $basedir/scripts node$i:$basedir/scripts
    scp -q -o StrictHostKeyChecking=accept-new $basedir/config/*.conf* node$i:$basedir/config
done

# Start servers
declare -A warehouses
while IFS=, read -r node ip
do
    ID=$(($ID+1));
    echo "starting server $ID on $node" >> $basedir/logs/execution.log;
    warehouses[$node]=$ID;
    ./scripts/sshserver.sh $node $basedir $ID $duration $clients $log $payload
    sleep .5;
done < <( awk '!/^ *#/ && NF' "$serverfile");
echo "started $servers servers"  >> $basedir/logs/execution.log;

lastnode="";
ID=0;
clifile=$basedir/config/clients.conf
while IFS=, read -r node nodewarehouse
do
    warehouse="${warehouses[$nodewarehouse]}"
    echo "$clispernode clients on $node assume as primary warehouse: $warehouse ($nodewarehouse)" >> $basedir/logs/execution.log;

    ./scripts/sshcli.sh $node $basedir $clients $ID $duration $locality $warehouse $msgs $log $clispernode $payload $thinktime $localm  # >> $basedir/logs/execution.log;
    sleep 1;
    ID=$(($ID+$clispernode));
    lastnode=$node;
done < <( awk '!/^ *#/ && NF'  "$clifile");
echo "started $clients clients" >> $basedir/logs/execution.log;

echo "waiting for nodes to finish" >> $basedir/logs/execution.log;
sleep "${duration}s";

while :
do
    while IFS=, read -r node ip
    do
        scp -q -r -o StrictHostKeyChecking=accept-new -o LogLevel=QUIET $node:$basedir/files/* $basedir/files/
    done < <( awk '!/^ *#/ && NF' "$serverfile");

    nodeFiles=`find $basedir/files -name 'NodeFinished*' | wc -l` #Count files and store in a variable
    if [ "$nodeFiles" -ge $servers ]; then break; fi
    sleep 2;
done

echo "all nodes done" >> $basedir/logs/execution.log;
./scripts/killAll.sh $nodes >> $basedir/logs/execution.log;
echo "experiment finished at " $(date)  >> $basedir/logs/execution.log;


# # copy logs to node0
# for i in $(seq 1 $nodes)
# do
#     scp -q -r -o StrictHostKeyChecking=accept-new -o LogLevel=QUIET node$i:$basedir/logs/* $basedir/logs/
# done

# for i in $(seq $(($servers+1)) $nodes)
# do
#     scp -q -r -o StrictHostKeyChecking=accept-new -o LogLevel=QUIET node$i:$basedir/results/* $basedir/results/
# done

# expdir="$basedir/experiments/${servers}nodes/${clients}cli/${locality}%";
# mkdir -p $expdir/config
# echo "moving data to" $expdir >> $basedir/logs/execution.log;
# cp -r $basedir/logs $expdir/
# cp -r $basedir/files $expdir/
# cp -r $basedir/results $expdir/
# cp    $basedir/config/*.conf* $expdir/config/
echo copying results/logs files to node0  >> $basedir/logs/execution.log;

./scripts/getResults.sh $clients $servers $nodes $locality

echo done. exiting  >> $basedir/logs/execution.log;
