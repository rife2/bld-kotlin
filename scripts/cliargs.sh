#!/bin/bash

main=org.jetbrains.dokka.MainKt

java -cp "lib/compile/*" $main -h |\
grep "    -" |\
sed -e "s/^    -/-/" -e "s/ \[.*//" -e "s/ ->.*//" -e '/help/d' |\
sort > "src/test/resources/dokka-args.txt"

java -cp "lib/compile/*" $main -sourceSet -h |\
grep "    -" |\
sed -e "s/^    -/-/" -e "s/ \[.*//" -e "s/ ->.*//" -e '/help/d' -e '/includeNonPublic/d' |\
sort > "src/test/resources/dokka-sourceset-args.txt"
