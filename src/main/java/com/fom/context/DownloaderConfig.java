package com.fom.context;

import com.fom.util.XmlUtil;

/**
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
	
	public boolean isDelSrc() {
		return delSrc;
	}

	public String getTempPath() {
		return tempPath;
	}

	public String getDestPath() {
		return destPath;
	}

	@Override
	void load() throws Exception {
		super.load();
		delSrc = XmlUtil.getBoolean(element, "src.del", false);
		tempPath = XmlUtil.getString(element, "temp.path", "");
		destPath = XmlUtil.getString(element, "dest.path", "");
	}

	@Override
	public final String getType() {
		return TYPE_DOWNLOADER;
	}

	@Override
	public final String getTypeName() {
		return NAME_DOWNLOADER;
	}

}
