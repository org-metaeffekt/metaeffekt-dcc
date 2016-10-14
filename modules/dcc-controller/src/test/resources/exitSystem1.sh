#!/bin/sh

java -Xmx1024m -cp . org.metaeffekt.dcc.shell.SystemExiter "1"
EXITED=$?
exit $EXITED
