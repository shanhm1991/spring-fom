#Fom
> 在公司呆的这一年时间里，发现由于项目性质，经常有关于文件操作的需求，每次就有童鞋临时写一个小工程部署，
> 这样导致质量参差不齐，代码和日志风格也是各种各样，长久下去维护起来可能会比较困难，
> 遂想私下写这样一个工具，希望能做到通用性，扩展性，也尽量提供一些util性质的工具

##功能（支持扩展）
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

##维护（http://ip:port/fom/html/index.html）
1. list: 
* 列出已加载的所有模块（包括信息：名称、类型、加载时间、启动时间、运行状态、配置是否合法），
* 点击可以查看加载的详细信息，并且可以查看、修改(实时生效)所加载的实际xml配置
2. srcs: 
* 根据模块名列出源文件目录下的匹配文件名或者所有文件名
3. logs: 
* 列出所有运行的日志，点击可以查看内容，或者下载
4. start/startAll/stop/stopAll/restart/restartAll：
* 启动/停止/重启模块

##examples
1. example_importLocalFileToEsByPool        解析本地txt/orc文件，使用内置pool方式导入es；
2. example_importLocalFileToMysqlByMybatis  解析本地txt/orc文件，使用mybatis方式导入mysql；
3. example_importLocalFileToMysqlByPool     解析本地txt/orc文件，使用内置pool方式导入mysql；
4. example_importLocalZipToOracleByMybatis  解析本地zip(txt/orc)文件，使用mybatis方式导入oracle；
5. example_importLocalZipToOracleByPool     解析本地zip(txt/orc)文件，使用内置pool方式导入oracle；
6. example_downloadHdfsFile     下载HDFS服务指定目录下文件；
7. example_downloadHdfsZip      下载HDFS服务指定目录下文件并打包；

##TODO
* 页面logs,提供日志级别设置功能
* 页面srcs,增加大小数目等统计信息
* 页面添加实时新增模块功能
* 安装包/rpm/注册系统服务
* jar方式提供


