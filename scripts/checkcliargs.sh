#!/bin/bash

new=/tmp/checkcliargs-new
old=/tmp/checkcliargs-old

kotlinc -h 2>$new
~/.sdkman/candidates/kotlin/2.1.0/bin/kotlinc -h 2>$old

code --diff --wait $old $new

rm -rf $new $old
