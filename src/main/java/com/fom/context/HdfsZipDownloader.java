package com.fom.context;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 * @param <E>
 */
public class HdfsZipDownloader<E extends HdfsZipDownloaderConfig> extends HdfsDownloader<E> {

	protected HdfsZipDownloader(String name, String path) {
		super(name, path);
	}

	
	
}
