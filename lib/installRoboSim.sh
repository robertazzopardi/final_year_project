if [ ! -d "comp329robosim" ]; then
  # Control will enter here if $DIRECTORY exists.
  jar xf comp329robosim.jar
fi

cd comp329robosim
rm -rf ./*.class
javac ./*.java
cd ..
jar cf comp329robosim.jar comp329robosim

mvn install:install-file \
-Dfile=comp329robosim.jar \
-DgroupId=comp329robosim \
-DartifactId=comp329robosim \
-Dversion=1.0 \
-Dpackaging=jar
