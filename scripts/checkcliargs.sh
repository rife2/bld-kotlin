#!/bin/bash

main=org.jetbrains.kotlin.cli.jvm.K2JVMCompiler

java -cp "lib/compile/*" $main -h 2>$new
java -cp "examples/lib/bld/*" $main -h 2>$old

diff $old $new

rm -rf $new $old
