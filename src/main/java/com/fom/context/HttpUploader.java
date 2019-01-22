package com.fom.context;

import com.fom.context.config.HttpUploaderConfig;
import com.fom.context.executor.Uploader;

/**
 * 
 * @author shanhm
 * @date 2019年1月10日
 *
 * @param <E>
 */
public class HttpUploader<E extends HttpUploaderConfig> extends Uploader<E> {

	protected HttpUploader(String name, String path) {
		super(name, path);
	}

	@Override
	protected void execUpload(E config) throws Exception {
		
	}

}
