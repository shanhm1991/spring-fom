package com.fom.context.config;

import java.io.File;

import com.fom.util.XmlUtil;

/**
 * <downloader.src.del>   源文件下载完是否删除
 * <downloader.withTemp>  是否使用临时目录，先下载到临时目录然后再转移到目标目录
 * <downloader.dest.path> 目标目录
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class DownloaderConfig extends Config {

	boolean delSrc;

	boolean withTemp;
	
	String destPath;
	
	String tempPath;

	protected DownloaderConfig(String name) {
		super(name);
	}
	
	@Override
	void load() throws Exception {
		super.load();
		delSrc = XmlUtil.getBoolean(element, "downloader.src.del", false);
		withTemp = XmlUtil.getBoolean(element, "downloader.withTemp", true);
		destPath = parseEnvStr(XmlUtil.getString(element, "downloader.dest.path", ""));
	}
	
	@Override
	boolean valid() throws Exception {
		if(!super.valid()){
			return false;
		}
		
		File dest = new File(destPath);
		if(!dest.exists() && !dest.mkdirs()){
			LOG.warn("无法创建目录[" + name + "]:" + destPath);
			return false;
		}
		if(withTemp){
			tempPath = System.getProperty("download.temp") + File.separator + name;
			File temp = new File(tempPath);
			if(!temp.exists() && !temp.mkdirs()){
				LOG.warn("无法创建目录[" + name + "]:" + tempPath);
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nsrc.del=" + delSrc);
		builder.append("\ndownloader.withTemp=" + withTemp);
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
				&& withTemp == c.withTemp
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

	public boolean isWithTemp() {
		return withTemp;
	}

	public String getDestPath() {
		return destPath;
	}

	public String getTempPath() {
		return tempPath;
	}
}
