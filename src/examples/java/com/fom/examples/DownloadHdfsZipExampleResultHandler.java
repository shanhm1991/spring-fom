package com.fom.examples;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.fom.context.Result;
import com.fom.context.ResultHandler;
import com.fom.log.LoggerAppender;
import com.fom.util.HdfsUtil;

public class DownloadHdfsZipExampleResultHandler implements ResultHandler {
	
	private String name;
	
	private String masterUrl;

	private String slaveUrl;
	
	private String sourceUri;
	
	private boolean isDelSrc;
	
	public DownloadHdfsZipExampleResultHandler(String name, 
			String masterUrl, String slaveUrl, String sourceUri, boolean isDelSrc){
		this.masterUrl = masterUrl;
		this.slaveUrl = slaveUrl;
		this.name = name;
		this.sourceUri = sourceUri;
		this.isDelSrc = isDelSrc;
	}

	@Override
	public void handle(Result result) throws Exception {
		log(name + ".record", result);
		if(!result.isSuccess() || !isDelSrc){
			return;
		}
		HdfsUtil.delete(masterUrl, slaveUrl, new Path(sourceUri));
	}
	
	private static void log(String logName, Result result){
		Logger logger = LogManager.exists(logName);
		if(logger == null){
			logger = Logger.getLogger(logName); 
			logger.setLevel(Level.INFO);  
			logger.setAdditivity(false); 
			logger.removeAllAppenders();
			LoggerAppender appender = new LoggerAppender();
			PatternLayout layout = new PatternLayout();  
			layout.setConversionPattern("%m%n");  
			appender.setLayout(layout); 
			appender.setEncoding("UTF-8");
			appender.setAppend(true);
			if(StringUtils.isBlank(System.getProperty("log.root"))){
				appender.setFile("log" + File.separator + logName);
			}else{
				appender.setFile(System.getProperty("log.root") + File.separator + logName);
			}
			appender.setRolling("false"); 
			appender.activateOptions();
			logger.addAppender(appender); 
		}
		StringBuilder builder = new StringBuilder("sourceUri=" + result.getSourceUri()
				+ ", result=" + result.isSuccess()
				+ ", startTime=" + result.getStartTime()
				+ ", costTime=" + result.getCostTime());
		if(result.getThrowable() == null){
			builder.append(", Throwable=null");
		}else{
			builder.append(", Throwable=" + result.getThrowable().getMessage());
		}
		logger.error(builder.toString()); 
	}

}
