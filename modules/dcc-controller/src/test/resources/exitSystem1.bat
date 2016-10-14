java -Xmx1024m -cp . org.metaeffekt.dcc.shell.SystemExiter "1"
set EXITED=%ERRORLEVEL%
echo %ERRORLEVEL%
exit /B %EXITED%
