package com.fom.context;

import com.fom.context.config.HttpDownloaderConfig;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class HttpDownloader<E extends HttpDownloaderConfig> extends Downloader<E> {

	protected HttpDownloader(String name, String path) {
		super(name, path);
	}

	@Override
	protected void download(E config) throws Exception {
		// TODO Auto-generated method stub
	}

}
