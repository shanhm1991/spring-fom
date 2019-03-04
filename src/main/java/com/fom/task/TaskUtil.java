package com.fom.task;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * 
 * @author shanhm
 *
 */
class TaskUtil {

	public static void checkInterrupt() throws InterruptedException{
		if(Thread.interrupted()){
			throw new InterruptedException("interrupted when batchProcessLineData");
		}
	}
	
	public static void log(Logger log, File logFile, int rowIndex) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("process progress: rowIndex=" + rowIndex);
		}
		FileUtils.writeStringToFile(logFile, String.valueOf(rowIndex), false);
	}
	
	public static void log(Logger log, File logFile, String name, int rowIndex) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("process progress: [" + name + "]rowIndex=" + rowIndex);
		}
		FileUtils.writeStringToFile(logFile, name + "\n" + rowIndex, false);
	}
}
