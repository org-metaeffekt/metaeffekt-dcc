#!/bin/sh

PRG="$0"

while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done
DCC_HOME=`dirname "$PRG"`

# Absolute path to folder which contains the folder which contains the shell.sh file
DCC_HOME=`cd "$DCC_HOME/.." ; pwd`

cd $DCC_HOME

# echo Resolved DCC_HOME: $DCC_HOME
# echo "JAVA_HOME $JAVA_HOME"

cygwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;
esac

# Build a classpath containing all libs
DCC_CP=`echo "$DCC_HOME"/lib/*.jar | sed 's/ \//:\//g'`
DCC_CP=`echo "$DCC_HOME"/config`:$DCC_CP
# echo DCC_CP: $DCC_CP

# Store file locations in variables to facilitate Cygwin conversion if needed

cygwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;
esac

JLINE_TERMINAL=
if [ "$cygwin" = "true" ]; then
    export DCC_HOME=`cygpath -wp "$DCC_HOME"`
    export DCC_CP=`cygpath -wp "$DCC_CP"`
    # echo "Modified DCC_HOME: $DCC_HOME"
    # echo "Modified DCC_CP: $DCC_CP"
    JLINE_TERMINAL=-Djline.terminal=jline.UnixTerminal
fi

# make sure to disable the flash message feature for the default OSX terminal, we recommend to use a ANSI compliant terminal such as iTerm if flash message support is desired
APPLE_TERMINAL=false;
if [ "$TERM_PROGRAM" = "Apple_Terminal" ]; then
        APPLE_TERMINAL=true
fi

#DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

ANSI="-Droo.console.ansi=true"
java $DEBUG -Xmx1024m -Duser.language=en -Dis.apple.terminal=$APPLE_TERMINAL $JLINE_TERMINAL $ANSI -cp "$DCC_CP" org.metaeffekt.dcc.shell.DccShell "$@"
SHELL_EXIT_CODE=$?
# echo DCC exited with code ${SHELL_EXIT_CODE}
exit ${SHELL_EXIT_CODE}