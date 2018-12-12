package com.fom.context;


/**
 * <src.path>
 * <src.pattern>
 * <src.match.fail.del>
 * <scanner.cron>
 * <scanner>
 * <executor>
 * <executor.min>
 * <executor.max>
 * <executor.aliveTime.seconds>
 * <executor.overTime.seconds>
 * <executor.overTime.cancle>
 * <downloader.src.del>
 * <downloader.temp.path>
 * <downloader.dest.path>
 * <hdfs1.url>
 * <hdfs2.url>
 * <signal.file>
 * <downloader.zip.content>
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
public class HdfsZipDownloaderConfig extends HdfsDownloaderConfig {
	
	int zipContent;
	
	protected HdfsZipDownloaderConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		
	}

	public int getZipContent() {
		return zipContent;
	}

	
}
