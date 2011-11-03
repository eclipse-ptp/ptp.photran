#!/bin/sh
RESULT=`which pdflatex 2>/dev/null | grep -v "no pdflatex in"`
if [ "$RESULT" == "" ]; then
  OS=`uname -s`
  if [ "$OS" == "Darwin" ]; then
    HARDWARE=`uname -m`
    if [ "$HARDWARE" == "i386" ]; then
      RESULT=`find /usr/local -name pdflatex | grep i386`
    elif [ "$HARDWARE" == "x86_64" ]; then
      RESULT=`find /usr/local -name pdflatex | grep x86_64`
    else
      RESULT=`find /usr/local -name pdflatex | grep ower`
    fi
  else
    RESULT=`find /usr/local/tex* -name pdflatex | head -1`
  fi
fi
if [ "$RESULT" == "" ]; then
  exit 1
else
  echo $RESULT
  exit 0
fi
