#概述
> 写这个工程是因为在之前的团队中，发现由于项目性质，经常有关于文件操作的小需求。而每次就有同学临时写一个小工程部署，
> 工程的质量参差不齐，代码和日志风格也是各种各样，经常会因为时间的紧迫性而将就质量， 这样毕竟不是长久之计，
> 遂决定写这样一个工具，希望以后能将这样的需求开发做到效率和质量兼顾，致力于通用可扩展的同时尽量做到简单好用。
> 开始初衷是为了解决生产消费式的文件操作需求，后来发现适用场景超过了自己的预期，fom更像是一个可以自定义实现的任务批处理管理。   

> fom本身是以servletListener的形式存在和启动的(实际加载和启动在spring容器加载结束时, 应用启动时会加载应用目录下所有名称带spring的xml文件)，
> 所以可以将它放在任何基于servlet实现的web服务中去，同时也是基于springboot写的，所以也可以以微服务的形式注册到springcloud中。
> fom中以context作为模块单位，context自定义了一套状态机制并将具体的任务执行委托给自己的线程池，对于模块状态及其任务线程池的配置和任务统计的提供了实时管理，
> 另外也提供了对模块自定义配置、日志级别等的实时管理，其次提供了一些常见文件操作的api和任务实现(一开始是为了解决文件批处理的需求)，例如上传、下载、解析。
> 对于text、orc以及excel文件读取，提供了统一的解析适配器Reader，使excel文件行读取像文本文件一样简单；
> 对于数据库操作提供了一个自定义实现的pool(支持jdbc和elasticsearch2.x)，可以实时响应配置的变更使得连接池配置的修改无需重启应用。
> fom的模块化没有通过定义类加载器做到依赖隔离，对于模块的新增，虽然可以做到从实时新增的jar包中加载Context实现，其实只是以事件的方式将手动添加的jar加到了classpath中，
> 勉强实现了这种能力，如果要替换jar等其他场景就无能为力了，后续希望能够基于OSGI做一个真正意义上的模块化管理。

##构建：基于maven
* fom-context: 运行模块任务的上下文定义
* fom-task: 常见的文件操作的任务实现
* fom-util: 一些不依赖上下文的静态工具
* fom-boot: 可以理解成在springboot中运行fom的驱动包，其实就是在springboot启动的监听列表中添加了fom上下文

##使用说明
1. 启动方式
* 依赖sprigboot启动
> 只需在启动类添加两个注解@Import({FomConfiguration.class})以及@ComponentScan(basePackages = {"org.eto.fom.boot"})即可，在spring-boot中附带了examples
* 以tomcat方式加载
> 需要在web.xml中的监听器列表中添加org.eto.fom.boot.listener.FomContextListener，可以看下main/resources/WEB-INF下保留的一开始以tomcat方式使用的web.xml等文件，
> 但是后来越改越偏向springboot了，可能需要做一些改动
* 自定义main函数启动类
> 需要调用org.eto.fom.context.ContextManager.load(xmlPath)加载配置文件，这样不需要引用fom-boot，因此也不存在监控页面访问了。

2. 启动参数
* -Dwebapp.root：设置资源根目录，默认将classpath作为根目录
* -Dcache.root：  设置缓存文件目录，默认为${webapp.root}/WEB-INF/cache（如果WEB-INF不存在则为${webapp.root}/cache）
* -Dlog.root：设置日志文件目录，默认为${webapp.root}/log
* -Dlog4jConfigLocation：设置log4j配置文件路径，默认位置：${webapp.root}/WEB-INF/log4j.properties
* -DfomConfigLocation： 设置fom配置文件路径，默认位置：${webapp.root}/WEB-INF/fom.xml
* -DpoolConfigLocation：设置pool配置文件路径，默认位置：${webapp.root}/WEB-INF/pool.xml

3. 配置
> 可以通过xml或者注解的形式配置自己的context模块

4. 运维(URL：http://ip:4040/fom.html)
> 可以实时监控模块的状态、配置以及统计信息；也可以实时新增模块（也可以加载新增jar包中的context）

##Examples
> 在fom-boot工程中，依赖springboot的方式，启动类：com.examples.Boot，打了个简单的tar包

##存在问题
* 利用CronExpression计算定时表达式，如果在秒分时天周中含有/，但表达的含义不是固定间隔的周期，将会被简单的误解成固定间隔周期；
* 其他问题，有待验证

##结语
> 限于个人水平和精力，虽然已经尽力反复检查和修改，难免仍有不足和疏漏之处，如果指正，感谢发送至shanhm1991@163.com
