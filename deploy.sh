user=zhengxiz
echo $user

host=linux.andrew.cmu.edu
echo $host

tar -cvf mprocess.tar ./*
ssh $user@$host "rm -rf ./private/MigratableProcess; mkdir ./private/MigratableProcess;"
scp mprocess.tar $user@$host:private/MigratableProcess
ssh $user@$host "cd ./private/MigratableProcess; tar -xvf mprocess.tar"
