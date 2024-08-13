

basedir=/usr/batista/skeen;
clients=$1;
servers=$2;
nodes=$3;
locality=$4;




# copy logs to node0
for i in $(seq 1 $nodes)
do
    scp -q -r -o StrictHostKeyChecking=accept-new -o LogLevel=QUIET node$i:$basedir/logs/* $basedir/logs/
done

for i in $(seq $(($servers+1)) $nodes)
do
    scp -q -r -o StrictHostKeyChecking=accept-new -o LogLevel=QUIET node$i:$basedir/results/* $basedir/results/
done

expdir="$basedir/experiments/${servers}nodes/${clients}cli/${locality}%";
mkdir -p $expdir/config

cp -r $basedir/logs $expdir/
cp -r $basedir/files $expdir/
cp -r $basedir/results $expdir/
cp    $basedir/config/*.conf* $expdir/config/