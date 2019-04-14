#概述
> 写这个工程是因为在之前的团队中，发现由于项目性质，经常有关于文件操作的小需求。而每次就有同学临时写一个小工程部署，
> 工程的质量参差不齐，代码和日志风格也是各种各样，经常会因为时间的紧迫性而将就质量， 这样毕竟不是长久之计，
> 遂决定写这样一个工具，希望以后能将这样的需求开发做到效率和质量兼顾，致力于通用可扩展的同时尽量做到简单好用。
> 开始初衷是为了解决生产消费式的文件操作需求，后来发现适用场景超过了自己的预期，一般的线程模型不是太复杂的小工程模块都可以使用fom来统一管理。

> fom本身是以servletListener的形式存在和启动的(实际加载和启动在spring容器加载结束时, 应用启动时会加载应用目录下所有名称带spring的xml文件)，
> 所以可以将它放在任何基于servlet实现的web服务中去，同时也是基于springboot写的，所以也可以以微服务的形式注册到springcloud中。
> fom中以context作为模块单位，context本质上是通过一个自定义的策略维护了一个定时和一个线程池，
> 围绕着这个定时策略和线程池做了一些状态、配置、统计等管理。其次提供了一些文件操作的api(因为一开始是为了解决文件操作需求)，
> 比如http、ftp以及hdfs的上传下载(打包)；将text、orc以及excel文件读取解析适配成统一的Reader.readRow()，尤其极大的简化了excel文件的读取；
> 另外对于数据库操作提供了一个自定义实现的pool(支持jdbc和elasticsearch2.x)，对于连接的管理肯定不如开源的上c3p0或者commons-pool等做得细化，
> 业务方法的支撑也不如它们丰富，但是做到了对于配置变更的实时响应，连接池的配置修改无需应用重启。

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
* 以springboot方式启动
> 修改启动类的注解，在@Import中添加FomConfiguration.class，以及在@ComponentScan注解中添加"com.fom"，即可
* 以tomcat方式部署
> 一开始是springbooy和tomcat部署都可以的，后来越改越偏向springboot了，
> 可能需要做一些改动，没有测过，在main/resources/WEB-INF下保留了原先的web.xml和springmvc.xml
* 自定义main方法
> com.fom.Context提供了启停方法，可以直接在main方法中启动自己的Context实现

3. 配置
> 可以通过xml或者注解的形式配置自己的context模块

4. 运维(URL：http://ip:4040/fom.html)
> 可以实时监控模块的状态、配置以及统计信息；也可以实时新增模块，能够不重启加载新增的jar中的context

##Examples
> package：com.examples；启动类：com.examples.boot.Boot

##存在问题
* 利用CronExpression计算定时表达式，如果在秒分时天周中含有/，但表达的含义不是固定间隔的周期，将会被简单的误解成固定间隔周期；
* 复杂配置项保存失败问题，比如配置项的值为xml，原因是无法转成json形式
* 其他问题，有待验证

##结语
> 限于个人水平和精力，虽然已经尽力反复检查和修改，难免仍有不足和疏漏之处，如果指正，感谢发送至shanhm1991@163.com
