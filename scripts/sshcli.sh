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

ssh -o StrictHostKeyChecking=accept-new $node "cd $basedir; ./scripts/runClients.sh \
$node $basedir $clients $ID $duration $locality $warehouse $msgs $log $clispernode $payload $thinktime $localm" &