#! /bin/sh
. /opt/FUDE/fude/profile/fude_profile

app_name="NetGuns_DataTransfer"
app_home="/home/nebula/NetGuns_DataTransfer"
main_class="com.fh.datatransfer.Boot"
guard_file="NetGuns_DataTransfer.xml"

JVM_OPTION="-Xms4096m -Xmx4096m -XX:NewRatio=1 -XX:SurvivorRatio=4 -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -verbose:gc -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTION="$JVM_OPTION -Dcom.sun.management.jmxremote"
JVM_OPTION="$JVM_OPTION -Dcom.sun.management.jmxremote.port=4096"
JVM_OPTION="$JVM_OPTION -Dcom.sun.management.jmxremote.ssl=false"
JVM_OPTION="$JVM_OPTION -Dcom.sun.management.jmxremote.authenticate=false"

SETCOLOR_SUCCESS="echo -en \\033[1;32m"  
SETCOLOR_FAILURE="echo -en \\033[1;31m"
SETCOLOR_WARNING="echo -en \\033[1;33m"
SETCOLOR_NORMAL="echo -en \\033[0;39m"

LogSuccess()
{
        time=`date "+%D %T"`
		$SETCOLOR_SUCCESS
		echo "[$time] [INFO]: $*"
		$SETCOLOR_NORMAL
}

LogInfo()
{
        time=`date "+%D %T"`
		echo "[$time] [INFO]: $*"
        $SETCOLOR_NORMAL
}

LogError()
{
        time=`date "+%D %T"`
		$SETCOLOR_FAILURE
		echo "[$time] [ERROR]: $*"
		$SETCOLOR_NORMAL
}

LogWarn()
{
        time=`date "+%D %T"`
		$SETCOLOR_WARNING
        echo "[$time] [WARN]: $*"
		$SETCOLOR_NORMAL
}

pwd=$(cd "$(dirname "$0")"; pwd)
start(){  
    if [ $app_home != $pwd ];then
	   LogError "run.sh[start] can only run in $app_name home directory: $app_home."
	   exit   
	fi
	
    pids=`ps -ef --width 4096|grep $app_name |grep -v grep |awk '{print $2}'`
	if [ ! -z "$pids" ];then
	   LogWarn "$app_name was already started[pid=$pids]."
	   exit
	fi
	
	LIBPATH=""
	LINE=`find ./lib -depth -name "*.jar"`
	for LOOP in $LINE
	do
		LIBPATH=$LIBPATH:$LOOP
	done
	
	s_time=`date "+%Y-%m-%d %H:%M:%S"`
	echo "start: $s_time">>$pwd/boot.log
    exec java $JVM_OPTION -cp "$app_name:$LIBPATH" $main_class $1 $2 $3 $4 $5 $6 $7 $8 1>>$pwd/boot.log 2>&1  &
	if [ ! $? == 0 ];then
	   LogWarn "$app_name start failed,see details in boot.log"
       exit
    fi 
	
	pids=`ps -ef --width 4096|grep $app_name |grep -v grep |awk '{print $2}'`
	if [ ! -z "$pids" ];then
	   sed -i 's/^start_time=.*/start_time="'"$s_time"'"/' run.sh
	   LogSuccess "$app_name start success[pid=$pids]."
	else
	   LogWarn "$app_name start failed,see details in boot.log"
	   exit
	fi
	
	dir_guard="/opt/FUDE/fude/etc/guard/conf.d"
	if [ -f $dir_guard/$guard_file ];then
	   LogInfo "add $app_name to fudeguard monitor."
	   fudeguardmgr.py --reload
	fi
}

stop(){
    if [ $app_home != $pwd ];then
	   LogError "run.sh[stop] can only run in $app_name home directory: $app_home."
	   exit   
	fi
	
    pids=`ps -ef --width 4096|grep $app_name |grep -v grep |awk '{print $2}'`
	if [ -z "$pids" ];then
	   LogWarn "$app_name was not running."
	   exit
	fi
	
	dir_guard="/opt/FUDE/fude/etc/guard/conf.d"
	if [ -f $dir_guard/$guard_file ];then
	   LogInfo "remove $app_name from fudeguard monitor."
	   fudeguardmgr.py --stopmonitor=NetGuns_DataTransfer
	fi
	
	s_time=`date "+%Y-%m-%d %H:%M:%S"`
	echo "stop: $s_time">>$pwd/boot.log
	for	pid in $pids
	do
        kill -15 $pid
        sleep 1
        if kill -0 $pid > /dev/null 2>&1; then
           kill -9 $pid
        fi
		LogSuccess "$app_name stoped[pid=$pid]."
    done
}

restart(){
    if [ $app_home != $pwd ];then
	   LogError "run.sh -restart can only run in $app_name home directory: $app_home."
	   exit   
	fi

    dir_guard="/opt/FUDE/fude/etc/guard/conf.d"
	if [ -f $dir_guard/$guard_file ];then
	   LogInfo "remove $app_name from fudeguard monitor."
	   fudeguardmgr.py --stopmonitor=NetGuns_DataTransfer
	fi
	
    pids=`ps -ef --width 4096|grep $app_name |grep -v grep |awk '{print $2}'`
	if [ ! -z "$pids" ];then
	   s_time=`date "+%Y-%m-%d %H:%M:%S"`
	   echo "stop: $s_time">>$pwd/boot.log
	   for	pid in $pids
	   do
           kill -15 $pid
           sleep 1
           if kill -0 $pid > /dev/null 2>&1; then
              kill -9 $pid
           fi
		   LogSuccess "$app_name stoped[pid=$pid]."
       done
	fi

    start
}

start_time="not started"
state(){	
    pids=`ps -ef --width 4096|grep $app_name |grep -v grep |awk '{print $2}'`
	if [ ! -z "$pids" ];then
	    echo "$app_name is running[pid=$pids]."	
    else
        echo "$app_name is dead."
    fi 
	
	if [ $app_home == $pwd ];then
	   echo "last start time: $start_time"
	fi
}

version=
build_time=
install_time="not installed"
version(){     
      echo "$app_name version: $version"
      echo "build time: $build_time"
	  echo "install time: $install_time"
}

export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

case "$1" in
    start)
    	start $2 $3 $4 $5 $6 $7 $8 $9
		;;
    stop)
    	stop $2 $3 $4 $5 $6 $7 $8 $9
		;;
    restart)
    	restart $2 $3 $4 $5 $6 $7 $8 $9
		;;
    version)
        version
		;;
    state)
		state
		;;
    *)
  	LogError $"usage: $0 {start|stop|restart|state|version}"
    exit 1
esac
exit 0
