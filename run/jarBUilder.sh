rm -rf artifacts/
mkdir artifacts/
cd artifacts/
mkdir -p ru/ifmo/rain/belyaev/implementor
mkdir -p info/kgeorgiy/java/advanced/implementor
cp ../ru/ifmo/rain/belyaev/implementor/*.java ru/ifmo/rain/belyaev/implementor 
cp ../info/kgeorgiy/java/advanced/implementor/*.class info/kgeorgiy/java/advanced/implementor


echo Manifest-Version: 1.0 >> Manifest.txt
echo Main-Class: ru.ifmo.rain.belyaev.implementor.Implementor >> Manifest.txt

javac ru/ifmo/rain/belyaev/implementor/*.java

jar cfm Implementor.jar Manifest.txt info/kgeorgiy/java/advanced/implementor/*.class ru/ifmo/rain/belyaev/implementor/*.class 

rm -rf ru/
rm -rf info/
rm -rf *.txt