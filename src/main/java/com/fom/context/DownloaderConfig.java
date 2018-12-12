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
 * <downloader.temp.path>
 * <downloader.dest.path>
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
public class DownloaderConfig extends Config {

	boolean delSrc;
	
	String tempPath;
	
	String destPath;

	protected DownloaderConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		delSrc = XmlUtil.getBoolean(element, "src.del", false);
		tempPath = XmlUtil.getString(element, "temp.path", "");
		destPath = XmlUtil.getString(element, "dest.path", "");
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
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nsrc.del=" + delSrc);
		builder.append("\ntemp.path=" + tempPath);
		builder.append("\ndest.path=" + destPath);
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof DownloaderConfig)){
			return false;
		}
		if(o == this){
			return true;
		}
		
		DownloaderConfig c = (DownloaderConfig)o; 
		if(!super.equals(c)){
			return false;
		}
		
		return delSrc = c.delSrc
				&& tempPath.equals(c.tempPath)
				&& destPath.equals(c.destPath);
	}

	@Override
	public final String getType() {
		return TYPE_DOWNLOADER;
	}

	@Override
	public final String getTypeName() {
		return NAME_DOWNLOADER;
	}

	public boolean isDelSrc() {
		return delSrc;
	}

	public String getTempPath() {
		return tempPath;
	}

	public String getDestPath() {
		return destPath;
	}
}
