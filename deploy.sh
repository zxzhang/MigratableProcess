tar -cvf mprocess.tar ./*
ssh zhengxiz@linux.andrew.cmu.edu "rm -rf ./private/MigratableProcess; mkdir ./private/MigratableProcess;"
scp mprocess.tar zhengxiz@linux.andrew.cmu.edu:private/MigratableProcess
ssh zhengxiz@linux.andrew.cmu.edu "cd ./private/MigratableProcess; tar -xvf mprocess.tar"
