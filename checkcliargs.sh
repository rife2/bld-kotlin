#!/bin/bash

TMPNEW=/tmp/checkcliargs-new
TMPOLD=/tmp/checkcliargs-old

cd lib/compile
java -cp "*" -jar dokka-cli-*.jar -h >$TMPNEW

cd ../../examples/lib/bld
java -cp "*" -jar dokka-cli-*.jar -h >$TMPOLD

diff $TMPOLD $TMPNEW

rm -rf $TMPNEW $TMPOLD
