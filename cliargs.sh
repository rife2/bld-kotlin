#!/bin/bash

java -cp "lib/compile/*" -jar lib/compile/dokka-cli-*.jar -h |\
grep "    -" |\
sed -e "s/   -/\"-/" -e "s/ \[.*//" -e "s/ -.*//" -e "s/\$/\",/" -e '/help/d' |\
sort |\
sed -e '$s/,//'

echo
echo ----------------------------------------
echo

java -cp "lib/compile/*" -jar lib/compile/dokka-cli-*.jar -sourceSet -h |\
grep "    -" |\
sed -e "s/   -/\"-/" -e "s/ \[.*//" -e "s/ -.*//" -e "s/\$/\",/" -e '/help/d' -e '/includeNonPublic/d' |\
sort |\
sed -e '$s/,//'
