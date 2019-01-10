package com.fom.context;

import com.fom.context.config.UploaderConfig;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public abstract class Uploader<E extends UploaderConfig> extends Executor<E> {

	protected Uploader(String name, String path) {
		super(name, path);
	}

	@Override
	protected final void exec(E config) throws Exception {
		execUpload(config);
	}

	protected abstract void execUpload(final E config) throws Exception;
}
