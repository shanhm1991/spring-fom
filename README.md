#概述
> 写这个程序是因为之前在项目组中，发现由于项目性质，经常有关于文件操作的需求，每次就有同学临时写一个小工程部署，
> 而工程的质量参差不齐，代码和日志风格也是各种各样，经常会因为时间的紧迫性而将就质量， 这样以后注定会有很多的风险，
> 遂决定私下写这样一个工具，希望能将这样的需求开发做到效率和质量兼顾，尽量做到简单好用，同时方便扩展。
> 一开始初衷是为了解决各种文件操作的需求，后来发现其他的需求也可以满足，本质上其实是一个生产消费模式。

##打包：mvn clean package -Dmaven.test.skip=true

##使用说明
1. 启动参数
* -Dwebapp.root：设置资源根目录，默认将classpath作为根目录
* -Dcache.root：  设置缓存文件目录，默认为${webapp.root}/WEB-INF/cache（如果WEB-INF不存在则为${webapp.root}/cache）
* -Dlog.root：设置日志文件目录，默认为${webapp.root}/log
* -Dlog4jConfigLocation：设置log4j配置文件路径，默认位置：${webapp.root}/WEB-INF/log4j.properties
* -DfomConfigLocation： 设置fom配置文件路径，默认位置：${webapp.root}/WEB-INF/fom.xml
* -DpoolConfigLocation：设置pool配置文件路径，默认位置：${webapp.root}/WEB-INF/pool.xml

2. 启动方式
* 以springboot方式启动，启动类：com.fom.boot.Application
> fom实现了功能模块化，状态和配置的实时管理，并提供简单的维护平台http://ip:4040/fom/index.html，只需将自己的业务功能实现为com.fom.Context便可以一键启动
* 以tomcat方式部署（未测）
> fom保留了web.xml和springmvc.xml，在main/resources/WEB-INF下面，对传统部署tomcat的方式作了兼容，可以以war包形式直接在tomcat中启动
* 自定义main方法启动
> com.fom.Context提供了启停方法，可以直接在main方法中启动自己的Context实现

##api支持
* fom另外提供了一些常见的文件操作策略，比如上次上传、下载（打包）、文件解析，实现方式有http、ftp、hdfs，
* 对于数据库操作除了使用开源的mybatis/hibernate外，另外也提供了一个自定义实现的pool（配置就是上面的pool.xml，支持mysql和oracle以及elasticsearch(2.x)的操作）
* 如果不以fom的Context作为启动入口，同样可以将fom当成一个工具包使用

##存在问题
* 计算定时周期是使用的org.quartz.CronExpression，但测试发现第一次获取的时间总是会提前
* ftpUtil未做测试
* 其他问题，有待验证
