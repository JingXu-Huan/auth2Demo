@echo off
echo Cleaning up old MyBatis files from IM-relationship-server...

cd src\main\java\org\example\imgroupserver

echo Renaming old Service...
if exist service\GroupService.java (
    ren service\GroupService.java GroupService.java.bak
    echo Renamed GroupService.java to GroupService.java.bak
)

echo.
echo Cleanup complete!
echo.
echo Next steps:
echo 1. Create Neo4jGroupService.java and FriendService.java
echo 2. Update Controllers to use new services
echo 3. Check ERROR_FIX_GUIDE.md for complete code
echo.
pause
