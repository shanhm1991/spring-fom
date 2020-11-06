package org.eto.fom.task.parse;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eto.fom.util.file.ZipUtil;

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
 * @see ParseTextTask
 * 
 * @param <V> 解析任务行数据解析结果类型
 * @param <E> 任务执行结果类型
 * 
 * @author shanhm
 * 
 */
public abstract class ParseTextZipTask<V, E> extends ParseTextTask<V, E> {

	private File unzipDir;

	private List<String> matchedEntrys;

	private String currentFileName;

	private int rowIndex = 0;

	/**
	 * @param sourceUri 资源uri
	 * @param batch 批处理数
	 */
	public ParseTextZipTask(String sourceUri, int batch) {
		super(sourceUri, batch);
	}

	@Override
	public boolean beforeExec() throws Exception {
		String sourceName = new File(id).getName();
		if(StringUtils.isBlank(getName())){
			this.progressLog = new File(System.getProperty("cache.parse") + File.separator + sourceName + ".log");
		}else{
			this.progressLog = new File(System.getProperty("cache.parse") 
					+ File.separator + getName() + File.separator + sourceName + ".log");
		}
		File parentFile = progressLog.getParentFile();
		if(!parentFile.exists() && !parentFile.mkdirs()){
			throw new RuntimeException("cache directory create failed: " + parentFile);
		}

		if(StringUtils.isBlank(getName())){
			this.unzipDir = new File(System.getProperty("cache.parse") + File.separator + sourceName);
		}else{
			this.unzipDir = new File(System.getProperty("cache.parse")
					+ File.separator + getName() + File.separator + sourceName);
		}

		if(!ZipUtil.valid(id)){ 
			log.error("zip invalid."); 
			if(!new File(id).delete()){
				log.error("zip delete failed."); 
			}
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
							log.error("delete file failed: {}", file.getName()); 
							return false;
						}
					}
				}
			}else{
				//首次任务处理
				if(!unzipDir.mkdirs()){
					log.error("directory create failed: {}", unzipDir);
					return false;
				}
			}

			long cost = ZipUtil.unZip(id, unzipDir);
			if (log.isDebugEnabled()) {
				log.debug("finish unzip({}KB), cost={}ms", formatSize(getSourceSize(id)), cost);
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
	public E exec() throws Exception {
		return parseFiles();
	}

	@Override
	public void afterExec(boolean isExecSuccess, E content, Throwable e) throws Exception {
		if(unzipDir.exists()){ 
			File[] fileArray = unzipDir.listFiles();
			if(!ArrayUtils.isEmpty(fileArray)){
				for(File file : fileArray){
					if(!file.delete()){
						log.warn("clear temp file failed: {}", file.getName()); 
						return;
					}
				}
			}
			
			if(!unzipDir.delete()){
				log.warn("clear temp directory failed."); 
				return;
			}
		}
		
		//srcFile.exist = true
		if(!deleteSource(id)){ 
			log.warn("clear src file failed."); 
			return;
		}
		
		if(progressLog.exists() && !progressLog.delete()){
			log.warn("clear progress log failed.");
		}
	}

	/**
	 * 匹配zip中的entry名称
	 * @param entryName entryName
	 * @return is matched 
	 */
	protected boolean matchEntryName(String entryName) {
		return true;
	}

	private List<String> filterEntrys(String[] nameArray){
		List<String> list = new LinkedList<>();
		for(String name : nameArray){
			if(matchEntryName(name)){
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
			log.warn("cann't find failed file: {}", currentFileName);
			return true;
		}

		long sTime = System.currentTimeMillis();
		try{
			rowIndex = Integer.valueOf(lines.get(1));
			log.info("get history processed progress: file={},rowIndex={}", currentFileName, rowIndex); 
		}catch(Exception e){
			log.warn("get history processed progress failed, will process from scratch.");
		}

		String path = unzipDir + File.separator + currentFileName;
		parseTxt(path, currentFileName, rowIndex); 
		matchedEntrys.remove(currentFileName);

		log.info("finish file[{}({}KB)], cost={}ms", currentFileName, formatSize(getSourceSize(path)), System.currentTimeMillis() - sTime);
		if(!file.delete()){
			log.error("delete file failed: {}", currentFileName); 
			return false;
		}
		return true;
	}

	private E parseFiles() throws Exception {
		Iterator<String> it = matchedEntrys.iterator();
		while(it.hasNext()){
			long sTime = System.currentTimeMillis();
			currentFileName = it.next();
			rowIndex = 0;

			String path = unzipDir + File.separator + currentFileName;
			parseTxt(path, currentFileName, rowIndex); 
			it.remove();

			File file = new File(unzipDir + File.separator + currentFileName);
			log.info("finish file[{}({}KB)], cost={}ms", currentFileName, formatSize(getSourceSize(path)), System.currentTimeMillis() - sTime);
			if(!file.delete()){ 
				log.error("delete file failed: {}", currentFileName); 
				throw new IllegalStateException("delete file failed: " + currentFileName);
			}
		}
		return onParseComplete();
	}
	
	@Override
	protected E onTextComplete(String sourceUri, String sourceName) throws Exception {
		return null;
	}
	
	/**
	 * zip解析完成时的动作
	 */
	protected abstract E onParseComplete() throws Exception;

}
