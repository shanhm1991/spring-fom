package com.fom.context.executor;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.Executor;
import com.fom.context.ResultHandler;
import com.fom.context.helper.UploaderHelper;

/**
 * 
 * @author shanhm
 *
 */
public class Uploader extends Executor {
	
	private File file;
	
	private String destUri;
	
	private boolean isDelSrc;
	
	private UploaderHelper helper;

	/**
	 * @param sourceUri sourceUri
	 * @param destUri destUri
	 * @param isDelSrc isDelSrc
	 * @param helper UploaderHelper
	 */
	public Uploader(String sourceUri, String destUri, boolean isDelSrc, UploaderHelper helper) {
		super(sourceUri);
		if(StringUtils.isBlank(sourceUri) || StringUtils.isBlank(destUri) || helper == null) {
			throw new IllegalArgumentException(); 
		}
		this.file = new File(sourceUri);
		this.destUri = destUri;
		this.isDelSrc = isDelSrc;
		this.helper = helper;
	}
	
	/**
	 * @param sourceUri sourceUri
	 * @param destUri destUri
	 * @param isDelSrc isDelSrc
	 * @param helper UploaderHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	public Uploader(String sourceUri, String destUri, 
			boolean isDelSrc, UploaderHelper helper, ExceptionHandler exceptionHandler) {
		this(sourceUri, destUri, isDelSrc, helper);
		this.exceptionHandler = exceptionHandler;
	}
	
	/**
	 * @param sourceUri sourceUri
	 * @param destUri destUri
	 * @param isDelSrc isDelSrc
	 * @param helper UploaderHelper
	 * @param resultHandler ResultHandler
	 */
	public Uploader(String sourceUri, String destUri, 
			boolean isDelSrc, UploaderHelper helper, ResultHandler resultHandler) {
		this(sourceUri, destUri, isDelSrc, helper);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param sourceUri sourceUri
	 * @param destUri destUri
	 * @param isDelSrc isDelSrc
	 * @param helper UploaderHelper
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	public Uploader(String sourceUri, String destUri, 
			boolean isDelSrc, UploaderHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(sourceUri, destUri, isDelSrc, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}
	
	@Override
	protected boolean onStart() throws Exception {
		if(!file.exists()){
			log.warn("file not exist.");
			return false;
		}
		return true;
	}

	@Override
	protected boolean exec() throws Exception {
		long sTime = System.currentTimeMillis();
		String size = new DecimalFormat("#.###").format(file.length());
		int code = helper.upload(file, destUri);
		if(code < 200 || code > 207){
			log.error("upload failed, code=" + code);
			return false;
		}
		log.info("finish upload(" + size + "KB, code=" + code + "), cost=" + (System.currentTimeMillis() - sTime) + "ms");
		return true;
	}
	
	@Override
	protected boolean onComplete() throws Exception {
		if(isDelSrc && !file.delete()){
			log.warn("delete file failed.");
			return false;
		}
		return true;
	}
	
}
