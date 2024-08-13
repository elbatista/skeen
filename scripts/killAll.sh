for i in $(seq $2 $(($2+$1-1)))
do
    ssh -o StrictHostKeyChecking=accept-new node$i "pkill -f 'java.*Main*'"
    echo killed node$i
done
echo killed all processes