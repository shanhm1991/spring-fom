package com.fom.context;

import org.apache.log4j.helpers.OptionConverter;

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
 * <downloader.zip.entry.max>
 * <downloader.zip.size.max>
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HdfsZipDownloaderConfig extends HdfsDownloaderConfig {
	
	String signalFile;
	
	int entryMax;
	
	long sizeMax;
	
	protected HdfsZipDownloaderConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		signalFile = XmlUtil.getString(element, "signal.file", "");
		entryMax = XmlUtil.getInt(element, "downloader.zip.entry.max", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
		String strSize = XmlUtil.getString(element, "downloader.zip.entry.max", "10GB");
		sizeMax = OptionConverter.toFileSize(strSize, 1024*1024*1024 * 10L);
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
		
		return entryMax == c.entryMax
				&& signalFile.equals(c.signalFile);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nsignal.file=" + signalFile);
		builder.append("\ndownloader.zip.entry.max=" + entryMax);
		return builder.toString();
	}
	
	@Override
	public final String getSignalFile() {
		return signalFile;
	}
	
}
