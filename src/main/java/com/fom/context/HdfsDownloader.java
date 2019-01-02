package com.fom.context;

import java.io.File;

import org.apache.hadoop.fs.Path;

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
		String dest = config.destPath;
		if(config.withTemp){
			dest = config.tempPath;
		}
		dest = dest + File.separator + srcName;

		config.fs.copyToLocalFile(config.delSrc, new Path(srcPath), new Path(dest), true);
		double size = new File(dest).length() / 1024.0;
		log.info("下载文件结束(" 
				+ numFormat.format(size) + "KB), 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
	}
}
