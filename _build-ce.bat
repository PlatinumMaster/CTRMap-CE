@echo off
set "ANT=C:\Program Files\Apache NetBeans\extide\ant\bin\ant.bat"
set "JDK=C:\Program Files\Java\jdk-1.8"
cd /D "D:\Games\Modding\NDS\CTRMap-CE"
call "%ANT%" -q "-Dplatforms.JDK_1.8.home=%JDK%" compile
