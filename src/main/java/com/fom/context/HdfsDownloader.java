package com.fom.context;

import java.io.File;

import com.fom.context.config.HdfsDownloaderConfig;
import com.fom.util.HdfsUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class HdfsDownloader<E extends HdfsDownloaderConfig> extends Downloader<E> {

	protected HdfsDownloader(String name, String path) {
		super(name, path);
	}

	@Override
	protected void execDownload(E config) throws Exception {
		long sTime = System.currentTimeMillis();
		String dest = config.getDestPath();
		if(config.isWithTemp()){
			dest = config.getTempPath();
		}
		dest = dest + File.separator + srcName;
		HdfsUtil.downloadAsFile(config.getFs(), config.isDelSrc(), srcPath, dest);
		log.info("下载文件结束(" 
				+ numFormat.format(srcSize) + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
	}
}
