rm -rf ./bin
mkdir ./bin
javac -classpath ./jar/reflections-0.9.9-RC1-uberjar.jar:./jar/com.google.common_1.0.0.201004262004.jar:./jar/javassist-3.8.0.GA.jar -d ./bin ./src/edu/cmu/courses/ds/launchingProcesses/*.java ./src/edu/cmu/courses/ds/mprocess/*.java ./src/edu/cmu/courses/ds/transactionalFileStream/*.java
