@echo off
echo Renaming old MyBatis Mappers to .bak...

cd src\main\java\org\example\imgroupserver\mapper

if exist GroupMapper.java (
    ren GroupMapper.java GroupMapper.java.bak
    echo Renamed GroupMapper.java to GroupMapper.java.bak
)

if exist GroupMemberMapper.java (
    ren GroupMemberMapper.java GroupMemberMapper.java.bak
    echo Renamed GroupMemberMapper.java to GroupMemberMapper.java.bak
)

echo Done!
pause
