TESTS=0
CODE=0

if [ $# -eq 0 ]; then
	sh update.sh --full
	exit
fi

while [ "$1" != "" ]; do
	case $1 in
        -t | --tests )          
                                TESTS=1
								;;
        -c | --code )  
								CODE=1
                                ;;
		-f | --full )  
								CODE=1
								TESTS=1
                                ;;
		
	esac
	shift
done

SOURCE="C:/Users/Roman/Desktop/University/Course_2/java/source/java-advanced-2019"
TESTS_FILES="$SOURCE/artifacts/*.jar"
LIBS="$SOURCE/lib/*.jar"

DESTINATION=$PWD

# update tests
if [ $TESTS -eq 1 ]; then
	( cd $SOURCE && git pull && git checkout -f HEAD )
	cp $TESTS_FILES $DESTINATION
	cp $LIBS $DESTINATION
fi

if [ $CODE -eq 1 ]; then 
	cp -r "../src/ru/" "./"
	cp -r "../src/info/" "./"
fi


