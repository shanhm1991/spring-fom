package com.fom.context;

import java.io.InputStream;
import java.util.List;

import com.fom.context.config.HttpZipDownloaderConfig;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class HttpZipDownloader<E extends HttpZipDownloaderConfig> extends ZipDownloader<E> {

	protected HttpZipDownloader(String name, String path) {
		super(name, path);
	}

	@Override
	protected List<String> getPathList() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected InputStream getResourceInputStream(String path) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected long getResourceSize(String path) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected boolean deletePath(String path) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

}
