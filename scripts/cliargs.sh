#!/bin/bash

java -cp "lib/compile/*" -jar lib/compile/dokka-cli-*.jar -h |\
grep "    -" |\
sed -e "s/^    -/-/" -e "s/ \[.*//" -e "s/ ->.*//" -e '/help/d' |\
sort > "src/test/resources/dokka-args.txt"

java -cp "lib/compile/*" -jar lib/compile/dokka-cli-*.jar -sourceSet -h |\
grep "    -" |\
sed -e "s/^    -/-/" -e "s/ \[.*//" -e "s/ ->.*//" -e '/help/d' -e '/includeNonPublic/d' |\
sort > "src/test/resources/dokka-sourceset-args.txt"
