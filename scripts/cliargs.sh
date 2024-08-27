#!/bin/bash

kotlinc -h 2> >(grep "^  ") |
  sed -e "s/^  //" -e "s/ .*//" -e "s/<.*//" -e '/-help/d' -e '/^-version/d' -e '/^$/d' |
  sort >"src/test/resources/kotlinc-args.txt"
