#Fom
> 在公司呆的这一年时间里，发现由于项目性质，经常有关于文件操作的需求，每次就有童鞋临时写一个小工程部署，
> 这样导致质量参差不齐，代码和日志风格也是各种各样，长久下去维护起来可能会比较困难，
> 遂想私下写这样一个工具，希望能做到通用性，扩展性，也尽量提供一些util性质的工具。

##功能
> Fom在上下文中对模块配置管理，任务线程分配作了统一处理，解决了可能出现的文件操作冲突、超时及异常问题，
> 实现了失败补偿和错误恢复以及断电续传等问题，另外提供一了个简单地运维页面，可以实时监控和修改各模块的运行状态和配置信息
> 目前实现的文件操作有解析入库、下载、上传，理论上应该可以实现任何关于文件的操作需求，暂时还没想到。
1. 解析入库
* 支持数据库：mysql、oracle、elasticsearch(2.x)；
* 支持入库方式：mybatis、内置pool；
* 支持文件来源：本地、HDFS服务、HTTP服务、FTP服务；
* 支持文件类型：文本、zip包；
* 支持文件文本格式：txt/orc、xml[TODO]；
2. 下载
* 支持下载方式：文件下载、文件打包下载；
* 支持下载服务：HDFS服务、HTTP服务、FTP服务；
3. 上传[TODO]
* 支持上传服务：HDFS服务、HTTP服务、FTP服务；

##维护（http://ip:port/.../index.html）
1. list: 
* 列出已加载的所有模块（包括信息：名称、类型、加载时间、启动时间、运行状态、配置是否合法），
* 点击可以查看加载的详细信息，并且可以查看、修改(实时生效)所加载的实际xml配置
2. srcs: 
* 根据模块名列出源文件目录下的匹配文件名或者所有文件名
3. logs: 
* 列出所有运行的日志，点击可以查看内容，或者下载
4. start/startAll/stop/stopAll/restart/restartAll：
* 启动/停止/重启模块

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
4. examples
* example_importLocalFileToEsByPool        解析本地txt/orc文件，使用内置pool方式导入es；
* example_importLocalFileToMysqlByMybatis  解析本地txt/orc文件，使用mybatis方式导入mysql；
* example_importLocalFileToMysqlByPool     解析本地txt/orc文件，使用内置pool方式导入mysql；
* example_importLocalZipToOracleByMybatis  解析本地zip(txt/orc)文件，使用mybatis方式导入oracle；
* example_importLocalZipToOracleByPool     解析本地zip(txt/orc)文件，使用内置pool方式导入oracle；
* example_downloadHdfsFile     下载HDFS服务指定目录下文件；
* example_downloadHdfsZip      下载并打包HDFS服务指定目录下文件；

##TODO
* 页面logs,提供日志级别设置功能
* 页面srcs,增加大小数目等统计信息
* 页面添加实时新增模块功能
* 安装包/rpm/注册系统服务


