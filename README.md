#概述
> 在公司呆的这一年时间里，发现由于项目性质，经常有关于文件操作的需求，每次就有同学临时写一个小工程部署，
> 这样导致质量参差不齐，代码和日志风格也是各种各样，毕竟不是长久之计，以后维护交接起来比较麻烦，
> 遂决定私下写这样一个工具，尽量做到简单好用，方便扩展以及比较通用。

> 可以将fom当作一个文件操作平台，它做到了模块化管理，以文件为独立单位创建任务线程，不同模块创建的任务线程提交各自独立的线程池。
> 模块配置支持xml和注解扫描，结合springboot，提供了web页面维护，可以查看或者实时修改模块状态以及配置，可以参考examples的实现。
> 也可以将fom当成一个工具jar，可以参考test中的例子，实现一个Context实例，使用executor包下面的策略，直接在main函数中启动即可。

##api支持 [document](http://htmlpreview.github.io/?https://github.com/shanhm1991/fom/tree/master/apidocs/index.html)
> fom提供了一些已实现好的操作策略，如常用的文件上传下载以及解析(考虑了失败恢复断点续传等问题)
1. 解析
* 文件来源：Local/HDFS/HTTP/FTP
* 文件类型：文本/zip包
* 文本格式：txt/orc/Excel
2. 下载/打包下载
* 文件服务器：HDFS/HTTP/FTP
3. 上传
* 文件服务器：HDFS/HTTP/FTP
4. 数据库
* 支持数据库：mysql/oracle/elasticsearch(2.x)
* 支持操作方式：mybatis/自定义pool

##web维护
* http://ip:4040/fom/index.html

##打包
* 在工程目录下执行cmd: mvn clean package(跳过测试：mvn clean package -Dmaven.test.skip=true)

##测试 (启动类：com.fom.boot.Application)
1. 启动参数
* -Dwebapp.root：设置应用即StandardContext的根目录(默认为class.getResource("/"))
* -Dcache.root：  设置缓存即应用处理过程中临时文件的根目录(默认为根目录下的WEB-INF/cache，如果找不到则设为根目录下的cache)
* -Dlog.root:    设置生成日志文件的根目录，默认为根目录下的log
* -Dlog4jConfigLocation：设置log4j配置文件路径，默认读取文件：/WEB-INF/log4j.properties
* -DfomConfigLocation：    设置fom配置文件路径，默认读取文件：/WEB-INF/fom.xml
* -DpoolConfigLocation：  设置pool配置文件路径，默认读取文件：/WEB-INF/pool.xml
2. 配置
* spring：应用启动时会加载所有目录带spring名字的xml文件，以及com.fom包下面所有的spring注解
* log4j：在/WEB-INF/log4j.properties或-Dlog4jConfigLocation指定的配置文件中进行配置
* fom:   在/WEB-INF/fom.xml或-DfomConfigLocation指定的配置文件中进行配置
* pool：  在/WEB-INF/pool.xml或-DpoolConfigLocation指定的配置文件中进行配置
3. 启动
* 在eclipse中启动：如果不设置-Dwebapp.root，应用会以eclipse的编译结果目录作为根目录
* 在tomcat容器中启动：工程中保留了web.xml和applicationContext.xml以及springnvc.xml就是为了兼容以往的web工程部署方式，将它们和需要的依赖文件一起以文件夹或war包形式放到tomcat目录下启动即可
* java命令启动：打成jar包和需要的依赖以及resource一起部署到目录下，通过java指定classpath启动

##TODO
* 页面添加实时新增模块功能
* 安装包/rpm/注册系统服务


