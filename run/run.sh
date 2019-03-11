
USER=''
TEST_CLASS=''
TASK_PACKAGE=''
SOLVE=''
CP_ONLY=0
RUN_HW_NUM=-1
TEST_USER=0
RUN_PARAMS=''
CURRENT_THREAD=0

len=$#

if [ $len -ne 0 ]; then
	echo "Run with params" $@
fi


while [ "$1" != "" ]; do
	case $1 in
        -u | --user )           shift
                                USER=$1
								RUN_PARAMS="$RUN_PARAMS --user $USER"
                                ;;
        -tp | --taskpackage )   shift
								TASK_PACKAGE=$1
                                RUN_PARAMS="$RUN_PARAMS --taskpackage $TASK_PACKAGE"
                                ;;
        -t | --testclass )      shift
								TEST_CLASS=$1
								RUN_PARAMS="$RUN_PARAMS --testclass $TEST_CLASS"
                                ;;
		-s | --solve )          shift
		                         
								SOLVE=$1
								RUN_PARAMS="$RUN_PARAMS --solve $SOLVE"
                                ;;
		-co | --compileonly )   
								CP_ONLY=1
								RUN_PARAMS="$RUN_PARAMS -co"
								;;
		-hw | --homeworknum )   shift
								RUN_HW_NUM=$1
								;;
		-tu | --testuser )   
								TEST_USER=1
								RUN_PARAMS="$RUN_PARAMS --user test"
								;;
		-ct | --currentthrerad ) # no such file   
								CURRENT_THREAD=1
								;;						
	esac
	shift
done

#run_hw hw_num params
run_hw(){
	echo "Run tests for hw$1"
	num=$1
	shift
	sh run$num.sh $@
}

max_hw_num(){
	files=$(ls *.sh)
	last=0
	for filename in $files 
	do
	  tmp=${filename//[A-Z.]/}
	  if [[ -n $tmp && $last -lt $tmp ]]; then
		last=$tmp
	  fi
	done;
	#echo "last $last"
	return $last
}

# if input args count less than 2 for start new with test user or compile only
if [[ $len -le 2 ]]; then
	add=''
	if [ $TEST_USER -ne 0 ]; then
		add="--user test"
	fi
	if [ $CP_ONLY -ne 0 ]; then
		add="$add -co"
	fi 
	max_hw_num
	run_hw $? $add
	exit
fi

# if script run with -hw arg
if [[ $RUN_HW_NUM -ne -1 && $CURRENT_THREAD -ne 1 ]]; then
	run_hw $RUN_HW_NUM $RUN_PARAMS
	exit
fi

javac -Xlint:unchecked ru/ifmo/rain/$USER/$TASK_PACKAGE/*.java 
if [ $CP_ONLY -ne 1 ]; then
 java -cp C:/Users/Roman/Desktop/University/Course_2/java/hw/run/info.kgeorgiy.kgeorgiy.java.advanced.implementor.jar;. -p . -m info.kgeorgiy.java.advanced.$TASK_PACKAGE $TEST_CLASS ru.ifmo.rain.$USER.$TASK_PACKAGE.$SOLVE
fi


