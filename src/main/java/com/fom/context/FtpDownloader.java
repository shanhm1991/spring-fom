package com.fom.context;

import com.fom.context.config.FtpDownloaderConfig;
import com.fom.context.executor.Downloader;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class FtpDownloader<E extends FtpDownloaderConfig> extends Downloader<E> {

	protected FtpDownloader(String name, String path) {
		super(name, path);
	}

	@Override
	protected void download(E config) throws Exception {
		// TODO Auto-generated method stub
	}

}
