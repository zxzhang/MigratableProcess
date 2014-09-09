sh package.sh
scp mprocess.tar zhengxiz@linux.andrew.cmu.edu:private/MigratableProcess
ssh zhengxiz@linux.andrew.cmu.edu "cd ./private/MigratableProcess; tar -xvf mprocess.tar"
