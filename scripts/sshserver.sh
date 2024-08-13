node=$1
basedir=$2
ID=$3
duration=$4
clients=$5
log=$6
payload=$7

ssh -o StrictHostKeyChecking=accept-new $node \
"cd $basedir; \
java -Xmx4024m -cp \"bin/*:lib/*\" MainServer -i $ID -d $duration -c $clients $log $payload >> $basedir/logs/node$ID.txt" & 