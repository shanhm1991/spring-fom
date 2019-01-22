package com.fom.context.executor;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fom.context.Context;
import com.fom.context.config.ImporterConfig;
import com.fom.context.exception.WarnException;
import com.fom.util.IoUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 * @param <V>
 */
public abstract class Importer<E extends ImporterConfig,V> extends Context<E> { 

	final File logFile;
	
	private boolean praseLineData_warned;
	
	private boolean batchProcessLineData_warned;

	protected Importer(String name, String path) {
		super(name, path);
		String logPath = System.getProperty("import.progress")
				+ File.separator + name + File.separator + srcName + ".log";
		this.logFile = new File(logPath);
	}

	@Override
	protected void exec(E config) throws Exception {
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
				if(config.getBatch() > 0 && lineDatas.size() >= config.getBatch()){
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

	//选择在每次批处理开始处检测中断，因为比较耗时的地方就两个(读取解析文件数据内容，数据入库)
	void batchProcessIfNotInterrupted(List<V> lineDatas, int lineIndex, long batchTime) throws Exception {
		if(interrupted()){
			throw new InterruptedException("processLine");
		}
		batchProcessLineData(config, lineDatas, batchTime); 
		log.info("批处理结束[" + lineDatas.size() + "],耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
	}
	
	/**
	 * 解析行数据, 异常则结束任务，保留文件，所以务必对错误数据导致的异常进行try-catch
	 * @param config
	 * @param line
	 * @param lineDatas
	 * @param batchTime
	 * @throws Exception
	 */
	public void praseLineData(E config, List<V> lineDatas, String line, long batchTime) throws Exception {
		if(!praseLineData_warned){
			praseLineData_warned = true;
		}
		log.warn("all line datas ignored,if you want to deal with it,you should override the method:"
				+ "[void praseLineData(E config, List<V> lineDatas, String line, long batchTime) throws Exception],"
				+ "in the method,you should prase the line string to a instance of V and add to the given list in the end.");
	}

	/**
	 * 批处理行数据解析结果, 异常则结束任务，保留文件
	 * @param lineDatas
	 * @param config
	 * @param batchTime
	 * @throws Exception
	 */
	public void batchProcessLineData(E config, List<V> lineDatas, long batchTime) throws Exception {
		if(!batchProcessLineData_warned){
			batchProcessLineData_warned = true;
		}
		log.warn("no data will be imported,you should override the method:"
				+ "[void batchProcessLineData(E config, List<V> lineDatas, long batchTime) throws Exception],"
				+ "and import the data which in the given list to the db.");
		
	}

	void updateLogFile(String fileName, int lineIndex) throws IOException{ 
		String data = fileName + "\n" + lineIndex;
		log.info("已读取行数:" + lineIndex);
		FileUtils.writeStringToFile(logFile, data, false);
	}
}
