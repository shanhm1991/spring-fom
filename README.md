#概述
> 写这个工程是因为在之前的团队中，发现由于项目性质，经常有关于离线数据操作的小需求，通常是以数据文件的形式，比如解析入库。
> 而每次总是临时写一个小程序部署，这样导致代码管理以及后续的维护都变的很困难，经常会因为时间成本问题而将就实现的质量。
> 遂决定写这样一个工具，希望以后能将类似需求开发做到效率和质量兼顾，并做到足够的通和可扩展，以及简单好用。

> fom本质上是基于线程池实现的 ，它以context作为模块单位，context中维护了一个线程池，以及一个内置线程，然后就可以通过context
> 就来控制任务的提交，并任务的执行委托给线程池。
> 以定时任务为例，使用fom可以做到任务的动态启动和关闭，以及配置实时更新，另外还可以查看任务执行的统计信息，日志级别实时修改等，
> 并且对任务配置的修改做了持久化，配置修改之后，下次重启依然生效。

##构建：基于maven
* fom-context: 模块任务的上下文定义
* fom-task: 给了一些常见的任务实现
* fom-util: 提供了一些不依赖上下文的工具类，比如对于text、orc以及excel的读取，适配了统一的Reader，使excel的读取可以如文本文件一样简单。
* fom-boot: 在springboot启动监听列表里加上了fom加载，其加载时机在spring组件加载完毕之后。
* fom-example: 写的一些使用示例

##使用说明
1. 使用方式
> 使用sprigboot的话，直接在启动类添加@ComponentScan(basePackages = {"org.eto.fom.boot"})即可，例如fom-example
> 部署tomcat的话，也可以在web.xml的<listener>中添加org.eto.fom.boot.listener.FomListener，
可以参考fom-boot/resources/WEB-INF/web.xml，可能要做一些修改适配

2. 启动参数
* -Dwebapp.root：设置资源根目录，默认将classpath作为根目录
* -Dcache.root：  设置缓存文件目录，默认为${webapp.root}/cache
* -DfomConfigLocation： 设置fom配置文件路径，默认位置：${webapp.root}/config/fom.xml
* -DpoolConfigLocation：设置pool配置文件路径，默认位置：${webapp.root}/config/pool.xml

3. 配置
> 支持通过xml或者注解的形式配置自己的任务

4. 运维(http://ip:4040/fom.html)
> 可以实时监控任务状态、统计信息，以及修改配置

##示例
> fom-example

##存在问题
* 有待验证

##结语
> 限于个人水平和精力，难免会有一些不足和错漏之处，如果有建议可以发送至shanhm1991@163.com
