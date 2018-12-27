package com.fom.context;

import com.fom.util.Utils;
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
 * <downloader.dest.path>
 * 
 * @author shanhm
 * @date 2018年12月23日
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
		destPath = Utils.parsePath(XmlUtil.getString(element, "downloader.dest.path", ""));
	}
	
	@Override
	boolean valid() throws Exception {
		if(!super.valid()){
			return false;
		}
		
		//校验 destPath
		
		tempPath = XmlUtil.getString(element, "temp.path", "");//TODO
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nsrc.del=" + delSrc);
		builder.append("\ndownloader.dest.path=" + destPath);
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

	public String getDestPath() {
		return destPath;
	}
}
