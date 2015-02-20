Paho Java client for MQTT

This is to be completed... (Ian Craggs)


Updating to a new version number
-------------------------------

Ok. There are some Maven commands to update releases, but so far I've not been able to determine what those should be.  

In the develop branch, we want the releases to be the vNext-SNAPSHOT 

But in the master branch, we want 


Maven command to update versions:

mvn versions:set -DnewVersion=1.0.2-SNAPSHOT

this will work for pom.xml files.  However we have OSGi manifests as well.  Linux commands to update versions:

find | grep "MANIFEST\.MF" | xargs sed -i "s/1\.0\.2/1\.0\.3\.qualifier/g"
find | grep "feature.xml" | xargs sed -i "s/1\.0\.2/1\.0\.3\.qualifier/g"
find | grep "build.xml" | xargs sed -i "s/1\.0\.2/1\.0\.3\.qualifier/g"
find | grep "category.xml" | xargs sed -i "s/1\.0\.2/1\.0\.3\.qualifier/g"
find | grep "ui.app.product" | xargs sed -i "s/1\.0\.2/1\.0\.3\.qualifier/g"

Example Linux command to find all files with instances of a version number:

find | xargs grep -s "1\.0\.2"
