package com.fom.context;

import com.fom.util.XmlUtil;

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
 * <downloader.withTemp>
 * <downloader.dest.path>
 * <hdfs1.url>
 * <hdfs2.url>
 * <signal.file>
 * <downloader.zip.content>
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HdfsZipDownloaderConfig extends HdfsDownloaderConfig {
	
	String signalFile;
	
	int zipContent;
	
	protected HdfsZipDownloaderConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		signalFile = XmlUtil.getString(element, "signal.file", "");
		zipContent = XmlUtil.getInt(element, "downloader.zip.content", 50, 1, 500);
	}
	
	@Override
	boolean valid() throws Exception {
		if(!super.valid()){
			return false;
		}
		//...
		return true;
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof HdfsZipDownloaderConfig)){
			return false;
		}
		if(o == this){
			return true;
		}
		
		HdfsZipDownloaderConfig c = (HdfsZipDownloaderConfig)o; 
		if(!super.equals(c)){
			return false;
		}
		
		return zipContent == c.zipContent
				&& signalFile.equals(c.signalFile);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nsignal.file=" + signalFile);
		builder.append("\ndownloader.zip.content=" + zipContent);
		return builder.toString();
	}
	
	@Override
	public final String getSignalFile() {
		return signalFile;
	}

	public int getZipContent() {
		return zipContent;
	}
	
}
