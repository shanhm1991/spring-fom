#概述
> 写这个项目是因为之前在团队中，发现由于项目性质，经常有关于文件操作的需求，每次就有同学临时写一个小工程部署，
> 而工程的质量参差不齐，代码和日志风格也是各种各样，经常会因为时间的紧迫性而将就质量， 这样以后注定会有很多的风险，
> 遂决定私下写这样一个工具，希望以后能将这样的需求开发做到效率和质量兼顾，尽量做到简单好用，同时方便扩展。
> 一开始初衷是为了解决各种文件操作的需求，其实一般的小工程（比如单独写个main函数启动的那种，线程模型相对简单，最好类似于生产消费模式），
> 都可以使用fom来统一管理，而且fom也可以以servletListener的形式合并到web服务中去

> fom本质上是基于线程池实现的功能模块管理，模块配置可以通过xml配置文件或者注解形式；同时提供了简单的
> 运维（URL：http://ip:4040/fom/index.html），可以实时管理模块的状态和配置信息，新增模块，管理任务统计信息及所有日志级别；
> 另外fom提供了一些常见的文件操作实现，因为一开始是针对文件操作写的（file operation manager），比如上传、下载（打包）、文件解析，实现方式有http、ftp、hdfs；
> 对于数据库操作除了使用开源的mybatis/hibernate外，fom也提供了一个自定义实现的pool（配置对应上面的pool.xml，支持mysql和oracle以及elasticsearch(2.x)的操作）；
> 所以即使不以fom的Context作为启动入口，同样可以将fom中的一些api当成工具包使用；

##打包：
* mvn clean package -Dmaven.test.skip=true

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
> 只需将自己的业务功能实现为com.fom.Context便可以一键启动
* 以tomcat方式部署（未测）
> fom保留了web.xml和springmvc.xml，在main/resources/WEB-INF下面，对传统部署tomcat的方式作了兼容，可以以war包形式直接在tomcat中启动
* 自定义main方法启动
> com.fom.Context提供了启停方法，可以直接在main方法中启动自己的Context实现

##存在问题
* 计算定时周期是使用的org.quartz.CronExpression，但测试发现第一次获取的时间总是会提前
* ftpUtil未做测试
* 其他问题，有待验证

##结语
> 由于个人水平和精力的限制，虽然已经尽力反复检查和修改，难免仍有不足和疏漏之处，如果指正，欢迎发送至shanhm1991@163.com，非常感谢
