#!/usr/bin/env bash
CP=./lib/libtensorflow-1.8.0.jar
CP="$CP:./build/classes/java/main"
echo "Classpath is $CP"
java -cp $CP -Djava.library.path=./lib/jni/org/tensorflow/native/darwin-x86_64 org.tensorflow.examples.LabelImage ./models/inception5h $*
