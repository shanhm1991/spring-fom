#!/bin/sh
. /opt/FUDE/fude/profile/fude_profile

app_name="NetGuns_DataTransfer"
app_home="/home/nebula/NetGuns_DataTransfer"
guard_file="NetGuns_DataTransfer.xml"

dir_base=`pwd`

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

check_jre(){
	if [ ! -d $JAVA_HOME ];then
        LogError "JRE not installed."
	    exit
	fi
	
	JAVA_VERSION=`java -version 2>&1 | grep version | awk -F '"' '{print $2}'`
	if [[ $JAVA_VERSION != 1.8.* ]];then
	    LogError "JRE version must be 1.8.x "
	    exit 1
    fi
    java -version
}

check_exist(){
    v_new=`sed -n '/^version=.*/p' run.sh 2>/dev/null | sed 's/version=//g' 2>/dev/null`
    if [ -z $v_new ];then
        LogError "unknown version,maybe the installation package is damaged."
        exit
    fi 
	LogInfo "prepare to install $app_name, version: $v_new"

    if [ -d $app_home ];then
        v_old=`sed -n '/^version=.*/p' $app_home/run.sh 2>/dev/null | sed 's/version=//g' 2>/dev/null`
        if [ -z $v_old ];then
            echo "$app_name was installed,but version is unknown,be sure to overwrite it with version $v_new anyway.[y/n]"
            read input
            if [ ! $input == "y" ];then
                exit
			else
			    uninstall
            fi
        else
            echo "$app_name version $v_old was already installed,be sure to overwrite it with verion $v_new.[y/n]"
            read input
            if [ ! $input == "y" ];then
                exit
			else
			    uninstall
            fi
        fi
    fi
}

install(){
    mkdir -p $app_home    	
    cp -rf release-note run.sh datasource.xml index.html application.properties lib WEB-INF $app_home
	
	install_time=`date "+%Y-%m-%d %H:%M:%S"`
	sed -i 's/^install_time=.*/install_time="'"$install_time"'"/' $app_home/run.sh
	LogSuccess "$app_name install success."

	dir_guard="/opt/FUDE/fude/etc/guard/conf.d"
	if [ ! -d $dir_guard ];then
        LogWarn "fudeguard home was't found, and $app_name will not be guard."
	else
	    rm -f $dir_guard/$guard_file
        cp -f $guard_file $dir_guard
    fi
	
	cd $app_home
	sh run.sh start
}

uninstall(){
    pids=`ps -ef --width 4096|grep $app_name |grep -v grep |awk '{print $2}'`
	if [ ! -z "$pids" ];then
		echo "$app_name is running now, be sure to stop it.[y/n]"
		read input
        if [ ! $input == "y" ];then
		    exit
        fi
	fi
	
	dir_guard="/opt/FUDE/fude/etc/guard/conf.d"
    if [ -f $dir_guard/$guard_file ];then
	    fudeguardmgr.py --stopmonitor=NetGuns_DataTransfer
        rm -f $dir_guard/$guard_file
		LogInfo "remove $app_name from fudeguard monitor."
    fi
			
	if [ ! -z "$pids" ];then
		for	pid in $pids
	    do
		installed_home=`pwdx $pid|awk -F ' ' '{print $2}'`
        kill -15 $pid
        sleep 1
        if kill -0 $pid > /dev/null 2>&1; then
          	kill -9 $pid
        fi
		LogSuccess "$app_name stoped[pid=$pid]."
		rm -rf $installed_home
		LogSuccess "$app_name cleared[home=$installed_home]."
        done  
	fi
	
	if [ -d $app_home ];then
	   rm -rf $app_home
       LogSuccess "$app_name cleared[home=$app_home]."
	fi
	LogSuccess "$app_name uninstall success."
}

function check_var()
{
    declare -a textArray
    textArray=()
	line=`cat globe.common.conf | grep $1=`
	value=`echo $line | cut -d= -f2`
	if [ -z $value ]; then
		for(( i=0; i<${#textArray[@]}; i++ ))
		do
			if [ $1 = ${textArray[$i]} ]; then
				return
			fi	
		done
		 LogWarn "the value of $1  is null!"
	else
		export $1=$value
	fi
}

function init()
{
    check_var pgsqlIp
    if [ -z $pgsqlIp ];then
       LogError "configuration[pgsqlIp] in globe.common.conf can't be empty."
       exit
	fi
	
	check_var pgsqlUser
    if [ -z $pgsqlUser ];then
       LogError "configuration[pgsqlUser] in globe.common.conf can't be empty."
       exit
	fi
	
	check_var pgsqlPasswd
    if [ -z $pgsqlPasswd ];then
       LogError "configuration[pgsqlPasswd] in globe.common.conf can't be empty."
       exit
	fi

	check_var SUPPLEMENT_IP
	if [ -z $SUPPLEMENT_IP ];then
       LogError "configuration[SUPPLEMENT_IP] in globe.common.conf can't be empty."
       exit
	fi
	
	check_var SUPPLEMENT_PORT
	if [ -z $SUPPLEMENT_PORT ];then
       LogError "configuration[SUPPLEMENT_PORT] in globe.common.conf can't be empty."
       exit
	fi
	
	check_var zkAddress
	if [ -z $zkAddress ];then
       LogError "configuration[zkAddress] in globe.common.conf can't be empty."
       exit
	fi
}

configuration() 
{
  init
  sed -i 's#netguns.config.supplement_ip=.*#netguns.config.supplement_ip='$SUPPLEMENT_IP'#g' application.properties
  sed -i 's#netguns.config.supplement_port=.*#netguns.config.supplement_port='$SUPPLEMENT_PORT'#g' application.properties
  sed -i 's#<zkAddress>.*#<zkAddress>'$zkAddress'</zkAddress>#g' WEB-INF/fom.xml
  
  sed -i 's#<property name="jdbcUrl" value=.*#<property name="jdbcUrl" value="jdbc:postgresql://'$pgsqlIp':5432/netgun" />#g' WEB-INF/input/spring_datasource_gunInput.xml
  sed -i 's#<property name="user" value=.*#<property name="user" value="'$pgsqlUser'" />#g' WEB-INF/input/spring_datasource_gunInput.xml
  sed -i 's#<property name="password" value=.*#<property name="password" value="'$pgsqlPasswd'" />#g' WEB-INF/input/spring_datasource_gunInput.xml
  
  sed -i 's#<driver-url>.*#<driver-url>jdbc:postgresql://'$pgsqlIp':5432/netgun</driver-url>#g' datasource.xml
  sed -i 's#<user>.*#<user>'$pgsqlUser'</user>#g' datasource.xml
  sed -i 's#<password>.*#<password>'$pgsqlPasswd'</password>#g' datasource.xml
}

case "$1" in
	install)
	    configuration
		check_jre
        check_exist
        install
		;;
	uninstall)
	    uninstall
		;;
	*)
	LogError $"usage: $0 {install|uninstall}"
	exit 1
esac
exit 0
