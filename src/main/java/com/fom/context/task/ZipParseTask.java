package com.fom.context.task;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.Task;
import com.fom.context.ResultHandler;
import com.fom.context.helper.ZipParseHelper;
import com.fom.context.reader.Reader;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 根据sourceUri解析本地单个zip包的任务实现
 * <br>
 * <br>解析策略：
 * <br>1.校验zip文件是否有效，如果无效则直接删除并且任务失败；检查缓存目录是否存在，没有则创建
 * <br>2.检查缓存目录下是否存在logFile（纪录任务处理进度）
 * <br>2.1如果存在，则判断解压目录是否存在
 * <br>2.1.1如果不存在，则进行步骤3
 * <br>2.1.2如果存在，则遍历过滤解压目录下的文件
 * <br>2.1.2.1如果过滤结果为空，则进行步骤3
 * <br>2.1.2.2如果过滤结果不为空，则先处理logFile中纪录的文件，然后逐个处理过滤的结果文件，最后进行步骤5
 * <br>2.2如果不存在，则判读解压目录是否存在，如果不存在则直接解压，否则清空解压目录后再重新解压，然后遍历过滤解压目录下的文件
 * <br>2.2.1如果过滤结果为空，则进行步骤3
 * <br>2.2.2如果过滤结果不为空，则逐个处理过滤的结果文件，最后进行步骤5
 * <br>3.执行清除
 * <br>3.1如果解压目录存在，则清空并删除目录
 * <br>3.2删除源文件
 * <br>3.3删除logFile
 * <br>上述任何步骤失败或异常均会使任务提前失败结束
 * <br>单个文件处理的说明：同理于ParseTask中
 * 
 * @see ZipParseHelper
 * @see ParseTask
 * 
 * @author shanhm
 * 
 */
public class ZipParseTask extends Task {

	private int batch;

	@SuppressWarnings("rawtypes")
	private ZipParseHelper helper;

	private File logFile;

	private File unzipDir;

	private List<String> matchedEntrys;

	private DecimalFormat numFormat  = new DecimalFormat("#.###");

	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 */
	@SuppressWarnings("rawtypes")
	public ZipParseTask(String sourceUri, int batch, ZipParseHelper helper) {
		super(sourceUri);
		this.helper = helper;
		String sourceName = new File(sourceUri).getName();

		if(StringUtils.isBlank(getContextName())){
			this.unzipDir = new File(System.getProperty("cache.parse") + File.separator + sourceName);
			this.logFile = new File(System.getProperty("cache.parse") + File.separator + sourceName + ".log");
		}else{
			this.unzipDir = new File(System.getProperty("cache.parse")
					+ File.separator + getContextName() + File.separator + sourceName);
			this.logFile = new File(System.getProperty("cache.parse") 
					+ File.separator + getContextName() + File.separator + sourceName + ".log");
		}
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	@SuppressWarnings("rawtypes")
	public ZipParseTask(String sourceUri, int batch, 
			ZipParseHelper helper, ExceptionHandler exceptionHandler) { 
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public ZipParseTask(String sourceUri, int batch, 
			ZipParseHelper helper, ResultHandler resultHandler) {
		this(sourceUri, batch, helper);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param exceptionHandler ExceptionHandler
	 * @param resultHandler ResultHandler
	 */
	@SuppressWarnings("rawtypes")
	public ZipParseTask(String sourceUri, int batch, 
			ZipParseHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	protected boolean beforeExec() throws Exception {
		if(!ZipUtil.valid(id)){ 
			log.error("zip invalid."); 
			if(!new File(id).delete()){
				log.error("zip delete failed."); 
			}
			return false;
		}

		File parentFile = logFile.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			log.error("directory create failed:" + parentFile);
			return false;
		}
		return true;
	}

	@Override
	protected boolean exec() throws Exception {
		if(logFile.exists()){
			log.warn("continue to deal with uncompleted task."); 
			//失败在第5步
			if(!unzipDir.exists()){ 
				return true;
			}
			String[] nameArray = unzipDir.list();
			//失败在第4步
			if(ArrayUtils.isEmpty(nameArray)){  
				return true;
			}
			matchedEntrys = filterEntrys(nameArray);
			//失败在第4步
			if(matchedEntrys.isEmpty()){
				log.warn("no file need to be process, clear directly.");
				return true;
			}
			//失败在第3步,继续处理未完成的文件
			if(!processFailedFile()){
				return false;
			}
		} else {
			if(unzipDir.exists()){
				File[] fileArray = unzipDir.listFiles();
				if(!ArrayUtils.isEmpty(fileArray)){
					//失败在第1、2步，清除并重新尝试
					log.warn("clear files which unzip uncorrectly.");
					for(File file : fileArray){
						if(!file.delete()){
							log.error("delete file failed: " + file.getName()); 
							return false;
						}
					}
				}
			}else{
				//首次任务处理
				if(!unzipDir.mkdirs()){
					log.error("directory create failed: " + unzipDir);
					return false;
				}
			}

			long cost = ZipUtil.unZip(id, unzipDir);
			if (log.isDebugEnabled()) {
				String size = numFormat.format(helper.getSourceSize(id));
				log.debug("finish unzip(" + size + "KB), cost=" + cost + "ms");
			}

			String[] nameArray = unzipDir.list();
			if(ArrayUtils.isEmpty(nameArray)){ 
				log.warn("no file need to be process, clear directly.");
				return true;
			}

			matchedEntrys = filterEntrys(nameArray);
			if(matchedEntrys.isEmpty()){
				log.warn("no file need to be process, clear directly.");
				return true;
			}

			if(!logFile.createNewFile()){
				log.warn("logFile create failed.");
				return false;
			}
		}
		return processFiles();
	}

	@Override
	protected boolean afterExec() throws Exception {
		if(unzipDir.exists()){ 
			File[] fileArray = unzipDir.listFiles();
			if(!ArrayUtils.isEmpty(fileArray)){
				for(File file : fileArray){
					if(!file.delete()){
						log.warn("clear temp file failed: " + file.getName()); 
						return false;
					}
				}
			}
			if(!unzipDir.delete()){
				log.warn("clear temp directory failed."); 
				return false;
			}
		}
		//srcFile.exist = true
		if(!helper.delete(id)){ 
			log.warn("clear src file failed."); 
			return false;
		}
		if(logFile.exists() && !logFile.delete()){
			log.warn("clear logFile failed.");
		}
		return true;
	}

	private List<String> filterEntrys(String[] nameArray){
		List<String> list = new LinkedList<>();
		for(String name : nameArray){
			if(helper.matchEntryName(name)){
				list.add(name);
			}
		}
		return list;
	}

	private boolean processFailedFile() throws Exception {  
		List<String> lines = FileUtils.readLines(logFile);
		if(lines.isEmpty()){
			return true;
		}

		String name = lines.get(0); 
		File file = new File(unzipDir + File.separator + name);
		if(!file.exists()){
			log.warn("cann't find failed file: " + name);
			return true;
		}

		long sTime = System.currentTimeMillis();
		int lineIndex = 0;
		try{
			lineIndex = Integer.valueOf(lines.get(1));
			log.info("get failed file processed progress[" + name + "]: " + lineIndex); 
		}catch(Exception e){
			log.warn("get failed file processed progress failed, will process from first line: " + name);
		}

		read(file.getPath(), lineIndex); 
		matchedEntrys.remove(name);

		if (log.isDebugEnabled()) {
			String size = numFormat.format(file.length() / 1024.0);
			log.debug("finish parse[" + name + "(" + size + "KB)], cost=" + (System.currentTimeMillis() - sTime) + "ms");
		}
		if(!file.delete()){
			log.error("delete file failed: " + name); 
			return false;
		}
		return true;
	}

	private boolean processFiles() throws Exception {
		Iterator<String> it = matchedEntrys.iterator();
		while(it.hasNext()){
			String name = it.next();
			long sTime = System.currentTimeMillis();
			File file = new File(unzipDir + File.separator + name);

			read(file.getPath(), 0);
			it.remove();

			if (log.isDebugEnabled()) {
				String size = numFormat.format(file.length() / 1024.0);
				log.debug("finish file[" + name + "(" + size + "KB)], cost=" + (System.currentTimeMillis() - sTime) + "ms");
			}
			if(!file.delete()){
				log.error("delete file failed: " + file.getName()); 
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void read(String uri, int StartLine) throws Exception {
		int lineIndex = 0;
		List<String> columns = null;
		Reader reader = null;
		try{
			reader = helper.getReader(uri);
			List batchData = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			while ((columns = reader.readLine()) != null) {
				lineIndex++;
				if(lineIndex <= StartLine){
					continue;
				}
				if(batch > 0 && batchData.size() >= batch && notInterruped()){
					int size = batchData.size();
					helper.batchProcessLineData(batchData, batchTime); 
					if (log.isDebugEnabled()) {
						log.debug("批处理结束[" + size + "],耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
					}
					logProcess(uri, lineIndex);
					batchData.clear();
					batchTime = System.currentTimeMillis();
				}
				helper.praseLineData(columns, batchData, batchTime);
			}
			if(!batchData.isEmpty() && notInterruped()){
				int size = batchData.size();
				helper.batchProcessLineData(batchData, batchTime); 
				if (log.isDebugEnabled()) {
					log.debug("批处理结束[" + size + "],耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
				}
			}
			logProcess(uri, lineIndex);
		}finally{
			IoUtil.close(reader);
		}
	}

	private boolean notInterruped() throws InterruptedException{
		if(Thread.interrupted()){
			throw new InterruptedException("interrupted when batchProcessLineData");
		}
		return true;
	}

	private void logProcess(String uri, int lineIndex) throws IOException{ 
		String data = uri + "\n" + lineIndex;
		if (log.isDebugEnabled()) {
			log.debug("rows processed: " + lineIndex);
		}
		FileUtils.writeStringToFile(logFile, data, false);
	}

}
