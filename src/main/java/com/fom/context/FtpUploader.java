package com.fom.context;

import com.fom.context.config.FtpUploaderConfig;
import com.fom.context.executor.Uploader;

/**
 * 
 * @author shanhm
 * @date 2019年1月10日
 *
 * @param <E>
 */
public class FtpUploader<E extends FtpUploaderConfig> extends Uploader<E> {

	protected FtpUploader(String name, String path) {
		super(name, path);
	}

	@Override
	protected void execUpload(E config) throws Exception {
		// TODO Auto-generated method stub
	}

}
