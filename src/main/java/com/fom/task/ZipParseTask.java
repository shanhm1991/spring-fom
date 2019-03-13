package com.fom.task;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.fom.context.ExceptionHandler;
import com.fom.context.ResultHandler;
import com.fom.context.Task;
import com.fom.task.helper.TextZipParseHelper;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.util.IoUtil;
import com.fom.util.ZipUtil;

/**
 * 根据sourceUri解析本地单个zip包的任务实现
 * <br>
 * <br>解析策略：
 * <br>1.校验zip文件是否有效，如果无效则直接删除并且任务失败；检查缓存目录是否存在，没有则创建
 * <br>2.检查缓存目录下是否存在progressLog（纪录任务处理进度）
 * <br>2.1如果存在，则判断解压目录是否存在
 * <br>2.1.1如果不存在，则进行步骤3
 * <br>2.1.2如果存在，则遍历过滤解压目录下的文件
 * <br>2.1.2.1如果过滤结果为空，则进行步骤3
 * <br>2.1.2.2如果过滤结果不为空，则先处理progressLog中纪录的文件，然后逐个处理过滤的结果文件，最后进行步骤5
 * <br>2.2如果不存在，则判读解压目录是否存在，如果不存在则直接解压，否则清空解压目录后再重新解压，然后遍历过滤解压目录下的文件
 * <br>2.2.1如果过滤结果为空，则进行步骤3
 * <br>2.2.2如果过滤结果不为空，则逐个处理过滤的结果文件，最后进行步骤5
 * <br>3.执行清除
 * <br>3.1如果解压目录存在，则清空并删除目录
 * <br>3.2删除源文件
 * <br>3.3删除progressLog
 * <br>上述任何步骤失败或异常均会使任务提前失败结束
 * <br>单个文件处理的说明：同理于ParseTask中
 * 
 * @see TextZipParseHelper
 * @see TxtParseTask
 * 
 * @param <V> 行数据解析结果类型
 * 
 * @author shanhm
 * 
 */
public abstract class ZipParseTask<V> extends Task {

	private int batch;

	private TextZipParseHelper helper;

	private File progressLog;

	private File unzipDir;

	private List<String> matchedEntrys;

	private DecimalFormat numFormat  = new DecimalFormat("#.###");

	private String currentFileName;

	private int rowIndex = 0;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 */
	public ZipParseTask(String sourceUri, int batch, TextZipParseHelper helper) {
		super(sourceUri);
		this.helper = helper;
		String sourceName = new File(sourceUri).getName();

		if(StringUtils.isBlank(getContextName())){
			this.unzipDir = new File(System.getProperty("cache.parse") + File.separator + sourceName);
			this.progressLog = new File(System.getProperty("cache.parse") + File.separator + sourceName + ".log");
		}else{
			this.unzipDir = new File(System.getProperty("cache.parse")
					+ File.separator + getContextName() + File.separator + sourceName);
			this.progressLog = new File(System.getProperty("cache.parse") 
					+ File.separator + getContextName() + File.separator + sourceName + ".log");
		}
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param exceptionHandler ExceptionHandler
	 */
	public ZipParseTask(String sourceUri, int batch, 
			TextZipParseHelper helper, ExceptionHandler exceptionHandler) { 
		this(sourceUri, batch, helper);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 * @param helper LocalZipParserHelper
	 * @param resultHandler ResultHandler
	 */
	public ZipParseTask(String sourceUri, int batch, 
			TextZipParseHelper helper, ResultHandler resultHandler) {
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
	public ZipParseTask(String sourceUri, int batch, 
			TextZipParseHelper helper, ExceptionHandler exceptionHandler, ResultHandler resultHandler) {
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

		File parentFile = progressLog.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			log.error("directory create failed:" + parentFile);
			return false;
		}

		if(progressLog.exists()){
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
			if(!parseFailedFile()){
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

			if(!progressLog.createNewFile()){
				log.warn("progress log create failed.");
				return false;
			}
		}

		return true;
	}

	@Override
	protected boolean exec() throws Exception {
		return parseFiles();
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
		if(progressLog.exists() && !progressLog.delete()){
			log.warn("clear progress log failed.");
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

	private boolean parseFailedFile() throws Exception {  
		List<String> lines = FileUtils.readLines(progressLog);
		if(lines.isEmpty()){
			return true;
		}

		currentFileName = lines.get(0); 
		File file = new File(unzipDir + File.separator + currentFileName);
		if(!file.exists()){
			log.warn("cann't find failed file: " + currentFileName);
			return true;
		}

		long sTime = System.currentTimeMillis();
		try{
			rowIndex = Integer.valueOf(lines.get(1));
			log.info("get history processed progress: file=" + currentFileName + ",rowIndex=" + rowIndex); 
		}catch(Exception e){
			log.warn("get history processed progress failed, will process from scratch.");
		}

		parse(); 
		matchedEntrys.remove(currentFileName);

		if (log.isDebugEnabled()) {
			String size = numFormat.format(file.length() / 1024.0);
			log.debug("finish file[" + currentFileName + "(" + size + "KB)], cost=" + (System.currentTimeMillis() - sTime) + "ms");
		}
		if(!file.delete()){
			log.error("delete file failed: " + currentFileName); 
			return false;
		}
		return true;
	}

	private boolean parseFiles() throws Exception {
		Iterator<String> it = matchedEntrys.iterator();
		while(it.hasNext()){
			long sTime = System.currentTimeMillis();
			currentFileName = it.next();
			rowIndex = 0;
			parse();
			it.remove();

			File file = new File(unzipDir + File.separator + currentFileName);
			String size = numFormat.format(file.length() / 1024.0);
			log.info("finish file[" + currentFileName + "(" + size + "KB)], cost=" + (System.currentTimeMillis() - sTime) + "ms");
			if(!file.delete()){ 
				log.error("delete file failed: " + currentFileName); 
				return false;
			}
		}
		return true;
	}

	private void parse() throws Exception {
		Reader reader = null;
		RowData rowData = null;
		try{
			reader = helper.getReader(unzipDir + File.separator + currentFileName);
			List<V> batchData = new LinkedList<>(); 
			long batchTime = System.currentTimeMillis();
			while ((rowData = reader.readRow()) != null) {
				if(rowIndex > 0 && rowData.getRowIndex() <= rowIndex){
					continue;
				}
				if(batch > 0 && batchData.size() >= batch){
					TaskUtil.checkInterrupt();
					int size = batchData.size();
					batchProcess(batchData, batchTime); 
					TaskUtil.log(log, progressLog, currentFileName, rowIndex); 
					batchData.clear();
					batchTime = System.currentTimeMillis();
					log.info("finish batch[file=" + currentFileName + ",size=" + size 
							+ "],cost=" + (System.currentTimeMillis() - batchTime) + "ms");
				}

				if (log.isDebugEnabled()) {
					log.debug("parse row[file=" + currentFileName + ",rowIndex= " 
							+ rowIndex + "],columns=" + rowData.getColumnList());
				}
				List<V> dataList = parseRowData(rowData, batchTime);
				if(dataList != null){
					batchData.addAll(dataList);
				}
			}
			if(!batchData.isEmpty()){
				TaskUtil.checkInterrupt();
				int size = batchData.size();
				batchProcess(batchData, batchTime); 
				TaskUtil.log(log, progressLog, currentFileName, rowIndex); 
				log.info("finish batch[file=" + currentFileName + ",size=" + size 
						+ "],cost=" + (System.currentTimeMillis() - batchTime) + "ms");
			}
		}finally{
			IoUtil.close(reader);
		}
	}

	/**
	 * 将行字段数据映射成对应的bean或者map
	 * @param rowData
	 * @param batchTime 批处理时间
	 * @return 映射结果V列表
	 * @throws Exception Exception
	 */
	public abstract List<V> parseRowData(RowData rowData, long batchTime) throws Exception;

	/**
	 * 批处理行数据
	 * @param batchData batchData
	 * @param batchTime batchTime
	 * @throws Exception Exception
	 */
	public abstract void batchProcess(List<V> batchData, long batchTime) throws Exception;
}
