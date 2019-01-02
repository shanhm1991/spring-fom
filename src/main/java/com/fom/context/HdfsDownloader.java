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
		String path = config.destPath;
		if(config.withTemp){
			path = config.tempPath;
		}
		config.fs.copyToLocalFile(config.delSrc, new Path(config.srcPath), new Path(path), true);
		double size = new File(path + File.separator + srcName).length() / 1024.0;
		log.debug("下载文件结束(" + numFormat.format(size) + "KB)");
	}
}
