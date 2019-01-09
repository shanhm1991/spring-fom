package com.fom.context;

import java.io.File;

import org.apache.hadoop.fs.Path;

import com.fom.context.config.HdfsDownloaderConfig;

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
	protected void download(E config) throws Exception {
		long sTime = System.currentTimeMillis();
		String dest = config.getDestPath();
		if(config.isWithTemp()){
			dest = config.getTempPath();
		}
		dest = dest + File.separator + srcName;

		config.getFs().copyToLocalFile(config.isDelSrc(), new Path(srcPath), new Path(dest), true);
		log.info("下载文件结束(" 
				+ numFormat.format(srcSize) + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
	}
}
