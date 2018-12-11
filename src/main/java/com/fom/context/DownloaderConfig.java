package com.fom.context;

import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm1991
 *
 */
public class DownloaderConfig extends Config {

	String tempPath;
	
	String destPath;

	boolean delSrc;

	protected DownloaderConfig(String name) {
		super(name);
	}

	@Override
	void load() throws Exception {
		super.load();
		tempPath = XmlUtil.getString(element, "src.path", "");
		destPath = XmlUtil.getString(element, "dest.path", "");
		delSrc = XmlUtil.getBoolean(element, "src.del", true);
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
