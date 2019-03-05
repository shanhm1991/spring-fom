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

	public static void log(Logger log, File progressLog, int rowIndex) throws IOException {
		log.info("process progress: " + rowIndex);
		FileUtils.writeStringToFile(progressLog, String.valueOf(rowIndex), false);
	}

	public static void log(Logger log, File progressLog, String name, int rowIndex) throws IOException {
		log.info("process progress: " + name + ", " + rowIndex);
		FileUtils.writeStringToFile(progressLog, name + "\n" + rowIndex, false);
	}
}
