#!/bin/bash

TMPNEW=/tmp/checkcliargs-new
TMPOLD=/tmp/checkcliargs-old

java -cp "lib/compile/*" -jar lib/compile/dokka-cli-*.jar -h >$TMPNEW
java -cp "/examples/lib/bld*" -jar examples/lib/bld/dokka-cli-*.jar -h >$TMPOLD

diff $TMPOLD $TMPNEW

java -cp "lib/compile/*" -jar lib/compile/dokka-cli-*.jar -sourceSet -h >$TMPNEW
java -cp "/examples/lib/bld*" -jar examples/lib/bld/dokka-cli-*.jar -sourceSet -h >$TMPOLD

diff $TMPOLD $TMPNEW


rm -rf $TMPNEW $TMPOLD
