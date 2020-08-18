#概述
一般提到多任务运行，会自然想到线程池，而定时任务则会想到spring schedule。那么定时多任务（定时执行一批任务）该怎么应付呢，      
一种解决办法是实例化一个线程池，然后用schedule起一个定时任务，将批量的任务提交到线程池中执行。     
但是如果，我还希望能够看到任务的执行状态，或者实时启停任务以及修改定时计划或者配置呢，这时就无能为力了。    

fom也是借助于线程池实现的，同时自定义了一个定时线程来控制任务的执行计划，这样就可以自由地动态介入改变任务的执行计划或其他配置了，    
并对修改做了持久化，下次重启依然生效。 类似于spring schedule，fom也提供了cron、fixedRate、fixedDelay三种定时策略，    
并且提供了onBatchComplete接口，相当于一个CompletionService。
最后，fom并不强制配置执行计划，如果没有配置，也可以当成一个纯粹的线程池使用    

写这个工具也是因为在的团队经常有类似任务的需求，但限于当时的出差条件，每次都是随便找个人临时开发，所以代码质量真的是一言难尽，      
遂决定写这样一个工具，希望以后能将类似需求开发做到效率和质量兼顾，且简单好用。

##构建
* fom-context: 相当于任务模块的上下文，负责创建和组织调度任务的执行；
* fom-task: 一些文件处理的任务实现，比如Excel或者zip的解析（考虑了一些失败重试机制，比如进程重启）；
* fom-util: 一些不依赖上下文的工具类，比如对Text、orc以及Excel的读取，适配了统一的Reader，使Excel的读取可以如文本文件一样简单；
* fom-boot: 将fom的加载放到了springboot的启动过程中， 具体在spring组件加载完毕之后；
* fom-example: 一些使用示例

##使用
```
<dependency>
	<groupId>org.eto</groupId>
	<artifactId>fom-boot</artifactId>
	<version>2.0.0</version>
</dependency>
```
配置包扫描路径`@FomScan(basePackages = "example.fom.fomschedulbatch")`，或者直接配置fom.xml

##示例
> fom-example

##界面
`http://{ip}:{port}/fom.html`或者`http://{ip}:{port}/{context-path}/task.html`    
查看任务执行状态和统计信息，以及实时进行启停和配置修改

##问题
* 待验证

##结语
> 限于个人水平和精力，难免会有一些不足和错漏之处，任何问题或者建议可以联系shanhm1991@163.com
