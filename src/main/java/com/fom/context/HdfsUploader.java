package com.fom.context;

import com.fom.context.config.HdfsUploaderConfig;
import com.fom.context.executor.Uploader;

/**
 * 
 * @author shanhm
 * @date 2019年1月10日
 *
 */
public class HdfsUploader<E extends HdfsUploaderConfig> extends Uploader<E> {

	protected HdfsUploader(String name, String path) {
		super(name, path);
	}

	@Override
	protected void execUpload(E config) throws Exception {
		// TODO Auto-generated method stub
	}

}
