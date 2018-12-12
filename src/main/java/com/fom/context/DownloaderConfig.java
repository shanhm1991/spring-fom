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
