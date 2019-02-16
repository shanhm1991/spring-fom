package com.fom.context.task;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.Task;
import com.fom.context.ResultHandler;
import com.fom.context.helper.UploaderHelper;

/**
 * 根据文件路径上传本地单个文件的任务实现，以文件路径作为task的id
 * <br>
 * <br>上传策略：
 * <br>1.判断本地文件是否存在，存在则进行下一步，否则任务失败
 * <br>2.上传文件
 * <br>3.决定是否删除源文件
 * <br>上述任何步骤失败或异常均会使任务提前失败结束
 * 
 * @author shanhm
 *
 */
public class Uploader extends Task {
	
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
	protected boolean beforeExec() throws Exception {
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
	protected boolean afterExec() throws Exception {
		if(isDelSrc && !file.delete()){
			log.warn("delete file failed.");
			return false;
		}
		return true;
	}
	
}
