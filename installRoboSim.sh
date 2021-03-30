cd lib

if [ ! -d "comp329robosim" ]; then
  # Control will enter here if $DIRECTORY exists.
  jar xf comp329robosim.jar
fi

cd comp329robosim
rm -rf ./*.class
# javac ./*.java
/Library/Java/JavaVirtualMachines/adoptopenjdk-16.jdk/Contents/Home/bin/javac ./*.java # set to the java version maven uses
cd ..
jar cf comp329robosim.jar comp329robosim

mvn install:install-file \
-Dfile=comp329robosim.jar \
-DgroupId=comp329robosim \
-DartifactId=comp329robosim \
-Dversion=1.0 \
-Dpackaging=jar

cd ..
