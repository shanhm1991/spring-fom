package com.fom.context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fom.util.IoUtils;
import com.fom.util.exception.WarnException;

/**
 * 
 * @author shanhm1991
 *
 * @param <E>
 * @param <V>
 */
public abstract class Importer<E extends ImporterConfig,V> extends Executor<E> { 

	final File logFile;

	protected Importer(String name, String path) {
		super(name, path);
		this.logFile = new File(path + ".log");
	}

	void execute() throws Exception {
		long sTime = System.currentTimeMillis();
		int lineIndex = 0;
		if(!logFile.exists()){
			if(!logFile.createNewFile()){
				throw new WarnException("创建日志文件失败.");
			}
		}else{
			log.warn("继续遗留任务处理."); 
			List<String> lines = FileUtils.readLines(logFile);
			try{
				lineIndex = Integer.valueOf(lines.get(1));
			}catch(Exception e){
				log.warn("获取文件处理进度失败,将从第0行开始处理.");
			}
		}

		readLine(srcFile, lineIndex);
		log.info("处理文件结束(" + srcSize + "KB),耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		if(!srcFile.delete()){
			throw new WarnException("删除文件失败."); 
		}
		if(!logFile.delete()){
			throw new WarnException("删除日志失败."); 
		}
	}

	void readLine(File file, int StartLine) throws Exception {
		int lineIndex = 0;
		BufferedReader reader = null;
		String line = "";
		try{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
			List<V> lineDatas = null; 
			int batch = config.batch;
			if(batch > 0){
				lineDatas = new ArrayList<V>(config.batch);
			}else{
				lineDatas = new ArrayList<V>(2500);
			}

			long batchTime = System.currentTimeMillis();
			while ((line = reader.readLine()) != null) {
				lineIndex++;
				if(lineIndex <= StartLine){
					continue;
				}

				if(batch > 0 && lineDatas.size() >= batch){
					batchProcessIfNotInterrupted(lineDatas, batchTime); 
					updateLogFile(file.getName(), lineIndex);
					lineDatas.clear();
					batchTime = System.currentTimeMillis();
				}
				lineDatas.add(praseLineData(config, line, batchTime));
			}
			if(!lineDatas.isEmpty()){
				batchProcessIfNotInterrupted(lineDatas, batchTime); 
				updateLogFile(file.getName(), lineIndex);
			}
		}finally{
			IoUtils.close(reader);
		}
	}

	/**
	 * 解析行数据, 异常则结束任务，保留文件，所以务必对错误数据导致的异常进行try-catch
	 * @param config
	 * @param line
	 * @param batchTime
	 * @return V
	 * @throws Exception
	 */
	protected abstract V praseLineData(E config, String line, long batchTime) throws Exception;

	//选择在每次批处理开始处检测中断，因为比较耗时的地方就两个(读取解析文件数据内容，数据入库)
	void batchProcessIfNotInterrupted(List<V> lineDatas, long batchTime) throws Exception {
		if(interrupted()){
			throw new InterruptedException("processLine");
		}
		batchProcessLineData(config, lineDatas, batchTime); 
	}

	/**
	 * 批处理行数据解析结果, 异常则结束任务，保留文件
	 * @param lineDatas
	 * @param config
	 * @param batchTime
	 * @throws Exception
	 */
	protected abstract void batchProcessLineData(E config, List<V> lineDatas, long batchTime) throws Exception;

	void updateLogFile(String fileName, int lineIndex) throws IOException{ 
		String data = fileName + "\n" + lineIndex;
		FileUtils.writeStringToFile(logFile, data, false);
	}
}
