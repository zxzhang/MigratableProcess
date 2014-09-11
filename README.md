MigratableProcess
=================

15640 project1, process migration

(1) You need copy the project to your AFS. You can use deploy.sh to copy the project to your own private/MigratableProcess folder. You should input your andrewID in deploy.sh file.

(2) You should deploy master machine first, and then deploy some slave machines. You should run runMaster.sh on a machine (hostname: ghc01.ghc.andrew.cmu.edu). Then, on some other machines, you should input master hostname (for example ghc01.ghc.andrew.cmu.edu) in runSlave.sh.

(3) You can just input test in master machine tiny shell command. Then, you can see some simple tests.

(4) Or, you can do some tests by yourself. Input “help” for help.

mig <processID> <hostname> : Migrage the process with <processID> to another machine(hostname).
run <processName> <args>   : Run a process in the machine.

         run GrepProcess <queryString> <inputFile> <outputFile>
         e.g. run GrepProcess 5 ./testCase/grepTest ./testCase/grepTest.out

         run ReplaceProcess <regexString> <replacementString> <inputFile> <outputFile>
         e.g. run ReplaceProcess 0 32123 ./testCase/replaceTest ./testCase/replaceTest.out

         run WordCountProcess <inputFile> <outputFile>
         e.g. run WordCountProcess ./testCase/wordTest ./testCase/wordTest.out
