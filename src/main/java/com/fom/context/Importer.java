package com.fom.context;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fom.util.IoUtil;
import com.fom.util.exception.WarnException;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 * @param <V>
 */
public abstract class Importer<E extends ImporterConfig,V> extends Executor<E> { 

	final File logFile;

	protected Importer(String name, String path) {
		super(name, path);
		String logPath = System.getProperty("import.progress")
				+ File.separator + name + File.separator + srcName + ".log";
		this.logFile = new File(logPath);
	}

	protected void execute(E config) throws Exception {
		long sTime = System.currentTimeMillis();
		int lineIndex = 0;
		if(!logFile.exists()){
			if(!logFile.createNewFile()){
				throw new WarnException("创建日志文件失败.");
			}
		}else{
			log.warn("继续处理任务遗留文件."); 
			List<String> lines = FileUtils.readLines(logFile);
			try{
				lineIndex = Integer.valueOf(lines.get(1));
				log.info("获取文件处理进度:" + lineIndex); 
			}catch(Exception e){
				log.warn("获取文件处理进度失败,将从第0行开始处理.");
			}
		}

		readFile(srcFile, lineIndex);
		log.info("处理文件结束(" + numFormat.format(srcSize) + "KB),耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		if(!srcFile.delete()){
			throw new WarnException("删除文件失败."); 
		}
		if(!logFile.delete()){
			throw new WarnException("删除日志失败."); 
		}
	}

	void readFile(File file, int StartLine) throws Exception {
		int lineIndex = 0;
		MultiReader reader = null;
		String line = "";
		try{
			reader = new MultiReader(file);
			List<V> lineDatas = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			while ((line = reader.readLine()) != null) {
				lineIndex++;
				if(lineIndex <= StartLine){
					continue;
				}
				if(config.batch > 0 && lineDatas.size() >= config.batch){
					batchProcessIfNotInterrupted(lineDatas, lineIndex, batchTime); 
					updateLogFile(file.getName(), lineIndex);
					lineDatas.clear();
					batchTime = System.currentTimeMillis();
				}
				praseLineData(config, lineDatas, line, batchTime);
			}
			if(!lineDatas.isEmpty()){
				batchProcessIfNotInterrupted(lineDatas, lineIndex, batchTime); 
			}
			updateLogFile(file.getName(), lineIndex);
		}finally{
			IoUtil.close(reader);
		}
	}

	/**
	 * 继承自Importer, 解析行数据, 异常则结束任务，保留文件，所以务必对错误数据导致的异常进行try-catch
	 * @param config
	 * @param line
	 * @param lineDatas
	 * @param batchTime
	 * @throws Exception
	 */
	protected abstract void praseLineData(E config, List<V> lineDatas, String line, long batchTime) throws Exception;

	//选择在每次批处理开始处检测中断，因为比较耗时的地方就两个(读取解析文件数据内容，数据入库)
	void batchProcessIfNotInterrupted(List<V> lineDatas, int lineIndex, long batchTime) throws Exception {
		if(interrupted()){
			throw new InterruptedException("processLine");
		}
		batchProcessLineData(config, lineDatas, batchTime); 
		log.info("批处理结束[" + lineDatas.size() + "],耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
	}

	/**
	 * 继承自Importer, 批处理行数据解析结果, 异常则结束任务，保留文件
	 * @param lineDatas
	 * @param config
	 * @param batchTime
	 * @throws Exception
	 */
	protected abstract void batchProcessLineData(E config, List<V> lineDatas, long batchTime) throws Exception;

	void updateLogFile(String fileName, int lineIndex) throws IOException{ 
		String data = fileName + "\n" + lineIndex;
		log.info("已读取行数:" + lineIndex);
		FileUtils.writeStringToFile(logFile, data, false);
	}
}
