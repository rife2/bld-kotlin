#!/bin/bash

java -cp "lib/compile/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -h 2> >(grep "^  ") |\
sed -e "s/^  //" -e "s/ .*//" -e "s/<.*//" -e '/-help/d' -e '/-version/d' -e '/^$/d'|\
sort > "src/test/resources/kotlinc-args.txt"

main=org.jetbrains.dokka.MainKt

java -cp "lib/compile/*" $main -h |\
grep "    -" |\
sed -e "s/^    -/-/" -e "s/ \[.*//" -e "s/ ->.*//" -e '/help/d' |\
sort > "src/test/resources/dokka-args.txt"

java -cp "lib/compile/*" $main -sourceSet -h |\
grep "    -" |\
sed -e "s/^    -/-/" -e "s/ \[.*//" -e "s/ ->.*//" -e '/help/d' -e '/includeNonPublic/d' |\
sort > "src/test/resources/dokka-sourceset-args.txt"
