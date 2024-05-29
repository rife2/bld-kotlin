#!/bin/bash

main=org.jetbrains.dokka.MainKt
new=/tmp/checkcliargs-new
old=/tmp/checkcliargs-old

java -cp "lib/compile/*" $main -h >$new
java -cp "examples/lib/bld/*" $main -h >$old

diff $old $new

java -cp "lib/compile/*" $main -sourceSet -h >$new
java -cp "examples/lib/bld/*" $main -sourceSet -h >$old

diff $old $new

main=org.jetbrains.kotlin.cli.jvm.K2JVMCompiler

java -cp "lib/compile/*" $main -h 2>$new
java -cp "examples/lib/bld/*" $main -h 2>$old

diff $old $new

rm -rf $new $old
