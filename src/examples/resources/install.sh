#!/bin/sh

app_name="fom-examples"
app_home="/home/fom-examples"

dir_base=`pwd`

check_jre(){
	if [ ! -d $JAVA_HOME ];then
        echo "[ERROR]: JRE not installed."
	    exit
	fi
    java -version
}

check_exist(){
    v_new=`sed -n '/^version=.*/p' release-note 2>/dev/null | sed 's/version=//g' 2>/dev/null`
    if [ -z $v_new ];then
        echo "[ERROR]: unknown version,maybe the installation package is damaged."
        exit
    fi 
	echo "[INFO]: prepare to install $app_name, version: $v_new"
	sed -i 's/^version=.*/version="'"$v_new"'"/' run.sh

    if [ -d $app_home ];then
        v_old=`sed -n '/^version=.*/p' $app_home/release-note 2>/dev/null | sed 's/version=//g' 2>/dev/null`
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
    cp -rf release-note run.sh application.properties lib WEB-INF source $app_home
	
	install_time=`date "+%Y-%m-%d %H:%M:%S"`
	sed -i 's/^install_time=.*/install_time="'"$install_time"'"/' $app_home/run.sh
	echo "[INFO]: $app_name install success."

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
	
	if [ ! -z "$pids" ];then
		for	pid in $pids
	    do
		installed_home=`pwdx $pid|awk -F ' ' '{print $2}'`
        kill -15 $pid
        sleep 1
        if kill -0 $pid > /dev/null 2>&1; then
          	kill -9 $pid
        fi
		echo "[INFO]: $app_name stoped[pid=$pid]."
		rm -rf $installed_home
		echo "[INFO]: $app_name cleared[home=$installed_home]."
        done  
	fi
	
	if [ -d $app_home ];then
	   rm -rf $app_home
       echo "[INFO]: $app_name cleared[home=$app_home]."
	fi
	echo "[INFO]: $app_name uninstall success."
}

case "$1" in
	install)
		check_jre
        check_exist
        install
		;;
	uninstall)
	    uninstall
		;;
	*)
	echo "[ERROR]: $usage: $0 {install|uninstall}"
	exit 1
esac
exit 0
