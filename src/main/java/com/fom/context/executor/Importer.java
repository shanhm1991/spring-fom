package com.fom.context.executor;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.fom.context.Executor;
import com.fom.context.executor.helper.AbstractImporterHelper;
import com.fom.context.executor.reader.Reader;
import com.fom.util.IoUtil;

/**
 * 
 * @author shanhm
 *
 */
public final class Importer extends Executor {

	private String sourceUri;

	private int batch;
	
	@SuppressWarnings("rawtypes")
	private AbstractImporterHelper helper;

	private File logFile;

	/**
	 * 
	 * @param name 模块名称
	 * @param sourceName 资源名称
	 * @param sourceUri 资源uri
	 * @param batch 入库时的批处理数
	 * @param helper ImporterHelper
	 */
	@SuppressWarnings("rawtypes")
	public Importer(String name, String sourceName, String sourceUri, int batch, AbstractImporterHelper helper){
		super(name,sourceName);
		this.sourceUri = sourceUri;
		this.batch = batch;
		this.helper = helper;
		this.logFile = new File(System.getProperty("import.progress") 
				+ File.separator + name + File.separator + sourceName + ".log");
	}
	
	@Override
	protected boolean onStart() throws Exception {
		File parentFile = logFile.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			log.error("创建目录失败:" + parentFile);
			return false;
		}
		return true;
	}

	@Override
	protected boolean exec() throws Exception {
		long sTime = System.currentTimeMillis();
		int lineIndex = 0;
		if(!logFile.exists()){ 
			if(!logFile.createNewFile()){
				log.error("创建日志文件失败.");
				return false;
			}
		}else{
			log.warn("继续处理失败任务."); 
			List<String> lines = FileUtils.readLines(logFile);
			try{
				lineIndex = Integer.valueOf(lines.get(0));
				log.info("获取文件处理进度:" + lineIndex); 
			}catch(Exception e){
				log.warn("获取文件处理进度失败,将从第0行开始处理.");
			}
		}
		read(lineIndex);
		String size = new DecimalFormat("#.##").format(helper.getSourceSize(sourceUri));
		log.info("处理文件结束(" + size + "KB),耗时=" + (System.currentTimeMillis() - sTime) + "ms");
		return true;
	}
	
	@Override
	protected boolean onComplete() throws Exception {
		if(!helper.delete(sourceUri)){ 
			log.error("删除源文件失败.");
			return false;
		}
		if(!logFile.delete()){
			log.error("删除日志文件失败.");
			return false;
		}
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void read(int StartLine) throws Exception {
		int lineIndex = 0;
		String line = "";
		Reader reader = null;
		try{
			reader = helper.getReader(sourceUri);
			List lineDatas = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			while ((line = reader.readLine()) != null) {
				lineIndex++;
				if(lineIndex <= StartLine){
					continue;
				}
				if(batch > 0 && lineDatas.size() >= batch){
					helper.batchProcessLineData(lineDatas, batchTime); 
					logProcess(sourceUri, lineIndex);
					lineDatas.clear();
					batchTime = System.currentTimeMillis();
				}
				helper.praseLineData(lineDatas, line, batchTime);
			}
			if(!lineDatas.isEmpty()){
				helper.batchProcessLineData(lineDatas, batchTime); 
			}
			logProcess(sourceUri, lineIndex);
		}finally{
			IoUtil.close(reader);
		}
	}

	private void logProcess(String uri, int lineIndex) throws IOException{ 
		log.info("已读取行数:" + lineIndex);
		FileUtils.writeStringToFile(logFile, String.valueOf(lineIndex), false);
	}
}
