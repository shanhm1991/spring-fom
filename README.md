#概述
> 一般提到多任务运行，我们都会想到线程池，而定时任务则会想到spring schedule。那么定时多任务（定时执行一批任务）该怎么应付呢，
> 一种解决办法是实例化一个线程池，然后用schedule起一个定时任务，将批量的任务提交到线程池中执行。
> 但是如果，我还希望能够看到任务的执行状态，或者实时启停任务以及修改定时计划或者配置呢，这时就无能为力了。

> fom也是借助于线程池实现的，同时自定义了一个定时线程来控制任务的执行计划，这样就可以自由地动态介入改变任务的执行计划或其他配置了，
> 类似于spring schedule，fom也提供了cron、fixedRate、fixedDelay三种定时策略，并且提供了onBatchComplete接口，相当于一个CompletionService。
> 另外，fom不强制配置执行计划，如果没有配置，同样可以当成一个纯粹的线程池使用

> 写这个工程是因为在之前的团队中，发现由于项目性质，经常有关于离线数据操作的小需求，通常是以数据文件的形式，比如解析入库。
> 而每次总是临时写一个小程序部署，这样导致代码管理以及后续的维护都变的很困难，经常会因为时间成本问题而将就实现的质量。
> 遂决定写这样一个工具，希望以后能将类似需求开发做到效率和质量兼顾，并做到足够的通用和可扩展，以及简单好用。

> fom本质上是基于线程池实现的 ，它以context作为模块单位，context中维护了一个线程池，以及一个内置线程，然后就可以通过context
> 就来控制任务的提交，并任务的执行委托给线程池。
> 以定时任务为例，使用fom可以做到任务的动态启动和关闭，以及配置实时更新，另外还可以查看任务执行的统计信息，日志级别实时修改等，
> 并且对任务配置的修改做了持久化，配置修改之后，下次重启依然生效。

##构建：基于maven
* fom-context: 模块任务的上下文定义
* fom-task: 给了一些常见的任务实现，比如Excel或者zip包的解析（考虑了一些容错机制，比如进程重启的问题）
* fom-util: 提供了一些不依赖上下文的工具类，比如对于Text、orc以及Excel的读取，适配了统一的Reader，使Excel的读取可以如文本文件一样简单
* fom-boot: 在springboot启动监听列表里加上了fom加载，其加载时机在spring组件加载完毕之后。
* fom-example: 使用示例

##使用说明
1. 使用方式
> 使用sprigboot的话，直接在启动类添加@ComponentScan(basePackages = {"org.eto.fom"})即可，例如fom-example
> 部署tomcat的话，也可以在web.xml的<listener>中添加org.eto.fom.boot.listener.FomListener，
可以参考fom-boot/resources/WEB-INF/web.xml，可能要做一些修改适配

2. 启动参数
* -Dwebapp.root：设置资源根目录，默认将classpath作为根目录
* -Dcache.root：  设置缓存文件目录，默认为${webapp.root}/cache
* -DfomConfigLocation： 设置fom配置文件路径，默认位置：${webapp.root}/config/fom.xml
* -DpoolConfigLocation：设置pool配置文件路径，默认位置：${webapp.root}/config/pool.xml

3. 配置
> 支持通过xml或者注解的形式配置自己的任务

##使用说明
> 以下面fom-example中的使用为例
> 添加依赖：
```
<dependency>
	<groupId>org.eto</groupId>
	<artifactId>fom-boot</artifactId>
	<version>2.0.0</version>
</dependency>
```
> 配置扫描包`@FomScan(basePackages = "example.fom.fomschedulbatch")`，或者直接在fom.xml中进行配置

##示例
> fom-example

##界面
> http://ip:port/fom.html或者http://ip:port/context-path/task.html
> 查看任务执行状态和统计信息，以及实时进行启停和配置修改

##存在问题
* 待验证

##结语
> 限于个人水平和精力，难免会有一些不足和错漏之处，如果有建议可以发送至shanhm1991@163.com
