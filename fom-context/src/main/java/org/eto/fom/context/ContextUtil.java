package org.eto.fom.context;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.eto.fom.context.ContextStatistics.CostDetail;
import org.eto.fom.util.IoUtil;

/**
 * context的操作api
 * 
 * @author shanhm
 *
 */
public class ContextUtil {

	private static final Logger LOG = Logger.getLogger(ContextUtil.class);

	private static final int BEGIN = 2;

	/**
	 * 替换字符串中的系统变量
	 * @param val string
	 * @return string
	 */
	public static String replaceSystemProperty(String val) {
		String begin = "${";
		char   end  = '}';
		int beginLen = BEGIN;
		int endLen  = 1;
		StringBuilder buffer = new StringBuilder();
		int i = 0;
		int j;
		int k;
		while(true) {
			j = val.indexOf(begin, i);
			if(j == -1) {
				if(i==0) {
					return val;
				} else { 
					buffer.append(val.substring(i, val.length()));
					return buffer.toString();
				}
			} else {
				buffer.append(val.substring(i, j));
				k = val.indexOf(end, j);
				if(k == -1) {
					throw new IllegalArgumentException('"' 
							+ val + "\" has no closing brace. Opening brace at position " + j + '.');
				} else {
					j += beginLen;
					String key = val.substring(j, k);
					String replacement = System.getProperty(key);
					if(replacement != null) {
						String recursiveReplacement = replaceSystemProperty(replacement);
						buffer.append(recursiveReplacement);
					}
					i = k + endLen;
				}
			}
		}
	}

	/**
	 * 提交task给对应名称为contextName的Context
	 * @param contextName contextName
	 * @param task task
	 * @return TimedFuture
	 * @throws Exception Exception
	 */
	public static <E> TimedFuture<Result<E>> submitTask(String contextName, Task<E> task) throws Exception { 
		Context context = ContextManager.contextMap.get(contextName);
		if(context == null){
			throw new IllegalArgumentException("context[" + contextName + "] not exist.");
		} 
		return context.submit(task);
	}

	/**
	 * 获取任务结果
	 * @param taskId taskId
	 * @param clzz clzz
	 * @return E
	 * @throws InterruptedException InterruptedException
	 * @throws ExecutionException ExecutionException
	 */
	@SuppressWarnings("unchecked")
	public static <E> E getTaskResult(String taskId, Class<E> clzz) throws InterruptedException, ExecutionException{ 
		TimedFuture<Result<?>> future = Context.FUTUREMAP.get(taskId);
		if(future == null){
			return null;
		}
		return (E)future.get().getContent();
	}

	/**
	 * 获取任务结果
	 * @param taskId taskId
	 * @param overTime overTime
	 * @param clzz clzz
	 * @return E
	 * @throws InterruptedException InterruptedException
	 * @throws ExecutionException ExecutionException
	 */
	@SuppressWarnings("unchecked")
	public static <E> E getTaskResult(String taskId, long overTime, Class<E> clzz)  
			throws InterruptedException, ExecutionException, TimeoutException{ 
		TimedFuture<Result<?>> future = Context.FUTUREMAP.get(taskId);
		if(future == null){
			return null;
		}
		return (E)future.get(overTime, TimeUnit.MILLISECONDS).getContent();
	}

	/**
	 * 获取Fom容器中所有的context统计信息
	 * @return map结果
	 */
	public static List<Map<String, String>> list() {
		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss SSS");
		List<Map<String, String>> list = new ArrayList<>();
		for(Entry<String, Context> entry : ContextManager.contextMap.entrySet()){
			Context context = entry.getValue();
			Map<String,String> cmap = new HashMap<>();
			cmap.putAll(context.config.valueMap); 

			cmap.put("name", context.name);
			cmap.put("state", context.getState().name().toLowerCase());
			if(!cmap.containsKey(ContextConfig.CRON)){
				cmap.put(ContextConfig.CRON, ""); 
			}
			
			cmap.put("executeTimes", String.valueOf(context.getExecuteTimes()));
			cmap.put("executeExceptions", String.valueOf(context.getExecuteExceptions()));
			
			String exec = "";
			if(context.execTime > 0){
				exec = format.format(context.execTime);
			}
			cmap.put("execTime", exec);
			cmap.put("loadTime", format.format(context.loadTime));
			cmap.put("level", context.getLogLevel());
			cmap.put("active", String.valueOf(context.getActives())); 
			cmap.put("waiting", String.valueOf(context.getWaitings()));
			cmap.put("failed", String.valueOf(context.statistics.getFailed()));
			cmap.put("failedDetails", String.valueOf(context.statistics.getfailedDetails()));
			cmap.put("success", String.valueOf(context.getSuccess()));

			cmap.remove("stopWithNoCron");
			list.add(cmap);
		}
		return list;
	}

	/**
	 * 更新context的配置项
	 * @param name context名称
	 * @param json json数据
	 * @return map结果
	 * @throws Exception Exception
	 */
	public static Map<String, Object> save(String name, String json) throws Exception {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		} 
		Map<String,String> bakMap = new HashMap<>();
		bakMap.putAll(context.config.valueMap);

		json = json.replaceAll("\\\\", "\\\\\\\\");
		JSONObject jsonObject = new JSONObject(json);

		@SuppressWarnings("unchecked")
		Iterator<String> it = jsonObject.keys();
		while(it.hasNext()){
			String key = it.next();
			String value = jsonObject.getString(key);
			switch(key){
			case ContextConfig.QUEUESIZE:
				context.config.setQueueSize(Integer.parseInt(value)); break;
			case ContextConfig.THREADCORE:
				context.config.setThreadCore(Integer.parseInt(value)); break;
			case ContextConfig.THREADMAX:
				context.config.setThreadMax(Integer.parseInt(value)); break;
			case ContextConfig.ALIVETIME:
				context.config.setAliveTime(Integer.parseInt(value)); break;
			case ContextConfig.OVERTIME:
				context.config.setOverTime(Integer.parseInt(value)); break;
			case ContextConfig.CANCELLABLE:
				context.config.setCancellable(Boolean.parseBoolean(value)); break;
			case ContextConfig.CRON:
				context.config.setCron(value); break;
			default:
				context.config.put(key, value);
			}
		}

		if(context.config.valueMap.equals(bakMap)){ 
			map.put("result", false);
			map.put("msg", "context[" + name + "] has nothing changed.");
			return map;
		}

		map.put("result", true);//已经更新成功
		if(serialize(name, context)){
			map.put("msg", "context[" + name + "] changed success.");
		}else{
			map.put("msg", "context[" + name + "] changed success, but save failed.");
		}
		return map;
	}

	private static boolean serialize(String name, Context context){
		String cache = System.getProperty("cache.context");
		File[] array = new File(cache).listFiles(new FileFilter(){
			@Override
			public boolean accept(File file) {
				return file.isFile();
			}
		});

		if(!ArrayUtils.isEmpty(array)){//将已有的缓存文件移到history
			for(File file : array){
				String fname = file.getName();
				if(name.equals(fname.substring(0, fname.lastIndexOf('.')))){ 
					File source = new File(cache + File.separator + fname);
					File dest = new File(cache + File.separator + "history" + File.separator + fname);
					if(!source.renameTo(dest)){
						LOG.error("context[" + name + "] move bak failed: " + fname);
						return false;
					}
					break;
				}
			}
		}

		String file = cache + File.separator + name + "." + System.currentTimeMillis();
		ObjectOutputStream out = null;
		try{
			out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(context);
			return true;
		}catch(Exception e){
			LOG.error("context[" + name + "] serialize failed.");
			return false;
		}finally{
			IoUtil.close(out);
		}
	}

	/**
	 * 启动
	 * @param name context名称
	 * @return map结果
	 */
	public static Map<String,Object> startup(String name) {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		return context.startup();
	}

	/**
	 * 停止
	 * @param name context名称
	 * @return map结果
	 */
	public static Map<String,Object> shutDown(String name){
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		return context.shutDown();
	}

	/**
	 * 立即运行
	 * @param name context名称
	 * @return map结果
	 */
	public static Map<String, Object> execNow(String name) {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		return context.execNow();
	}

	/**
	 * 获取context状态
	 * @param name context名称
	 * @return map结果
	 */
	public static Map<String, Object> state(String name) {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		map.put("result", true);
		map.put("state", context.getState().name().toLowerCase());
		return map;
	}

	/**
	 * 新建context模块
	 * @param json json数据
	 * @return map结果
	 * @throws Exception Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String, Object> create(String json) throws Exception {
		Map<String, Object> resMap = new HashMap<>();

		json = json.replaceAll("\\\\", "\\\\\\\\");
		JSONObject jsonObject = new JSONObject(json);

		Map<String,String> map = new HashMap<>();
		Iterator<String> it = jsonObject.keys();
		while(it.hasNext()){
			String key = it.next();
			map.put(key, jsonObject.getString(key));
		}

		String clazz = map.get("class");
		if(StringUtils.isBlank(clazz)){
			resMap.put("result", false);
			resMap.put("msg", "class cann't be empty.");
			return resMap;
		}

		Loader.refreshClassPath();//如果有添加新的jar包，则先刷新下

		Class<?> contextClass = null;
		try {
			contextClass = Class.forName(clazz);
			if(!Context.class.isAssignableFrom(contextClass)){
				resMap.put("result", false);
				resMap.put("msg", clazz + " ism't subclass of com.fom.context.Context");
				return resMap;
			}
		}catch(Exception e){
			LOG.error(clazz + "load failed", e);
			resMap.put("result", false);
			resMap.put("msg", clazz + " load failed.");
			return resMap;
		}

		String name = map.get("name");
		boolean isNameEmpty = false; 
		if(StringUtils.isBlank(name)){
			isNameEmpty = true;
			FomContext fc = contextClass.getAnnotation(FomContext.class);
			if(fc != null && !StringUtils.isBlank(fc.name())){
				name = fc.name();
			}else{
				name = contextClass.getSimpleName();
			}
		}

		if(ContextManager.exist(name)){
			LOG.warn("context[" + name + "] already exist, create canceled.");
			resMap.put("result", false);
			resMap.put("msg", "context[" + name + "] already exist, create canceled.");
			return resMap;
		}

		ContextManager.createMap.put(name, map);
		Context context = null;
		if(isNameEmpty){ 
			context = (Context)contextClass.newInstance();
		}else{
			Constructor constructor = contextClass.getConstructor(String.class);
			context = (Context)constructor.newInstance(name);
		}
		context.regist();
		context.startup();
		resMap.put("result", true);

		if(serialize(name, context)){
			resMap.put("msg", "context[" + name + "] create success.");
		}else{
			resMap.put("msg", "context[" + name + "] create success, but save failed.");
		}
		return resMap;
	}

	/**
	 * 修改context日志级别
	 * @param name context名称
	 * @param level 日志级别
	 */
	public static void changeLogLevel(String name, String level) {
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return;
		}
		context.changeLogLevel(level);
	}

	/**
	 * 获取context之外的log级别
	 * @return log名称与级别
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, String> listOtherLogs() {
		Enumeration loggerEnumeration = LogManager.getLoggerRepository().getCurrentLoggers();
		Map<String, String> map = new HashMap<>();
		Set<String> listed = new HashSet<>();
		while(loggerEnumeration.hasMoreElements()){
			Logger logger = (Logger)loggerEnumeration.nextElement();
			if(ContextManager.contextMap.containsKey(logger.getName())){  
				continue;
			}
			listLog(logger, map, listed);
		}
		return map;
	}

	@SuppressWarnings("rawtypes")
	private static void listLog(Category logger, Map<String, String> map, Set<String> listed){
		String loggerName = logger.getName();
		if(listed.contains(loggerName)){
			return;
		}

		Enumeration appenderEnumeration = logger.getAllAppenders(); 
		if(!appenderEnumeration.hasMoreElements()){
			if(!logger.getAdditivity()){
				return;
			}else if(logger.getParent() == Logger.getRootLogger()){
				appenderEnumeration = Logger.getRootLogger().getAllAppenders(); 
				StringBuilder builder = new StringBuilder();
				while(appenderEnumeration.hasMoreElements()){
					builder.append(((Appender)appenderEnumeration.nextElement()).getName() + ",");
				}
				String append = builder.toString();
				append = append.substring(0, append.length() - 1);

				Level level = logger.getLevel();
				if(level == null){
					level = Logger.getRootLogger().getLevel();
				}

				map.put(loggerName + "[" + append + "]", level.toString());
				listed.add(loggerName);
				return;
			}
			listLog(logger.getParent(), map, listed);
		}else{
			StringBuilder builder = new StringBuilder();
			while(appenderEnumeration.hasMoreElements()){
				builder.append(((Appender)appenderEnumeration.nextElement()).getName() + ",");
			}
			String append = builder.toString();
			append = append.substring(0, append.length() - 1);
			Level level = logger.getLevel();

			map.put(loggerName + "[" + append + "]", level.toString());
			listed.add(loggerName);
		}
	}

	/**
	 * 查询日志级别
	 * @param loggerName loggerName
	 * @return level
	 */
	@SuppressWarnings("rawtypes")
	public static String queryLogLevel(String loggerName) {
		Enumeration loggerEnumeration = LogManager.getLoggerRepository().getCurrentLoggers();
		Map<String, String> map = new HashMap<>();
		Set<String> listed = new HashSet<>();
		while(loggerEnumeration.hasMoreElements()){
			Logger logger = (Logger)loggerEnumeration.nextElement();
			if(ContextManager.contextMap.containsKey(logger.getName())){  
				continue;
			}
			listLog(logger, map, listed);
		}
		String level = map.get(loggerName);
		if(level == null){
			level = "NULL";
		}
		return level;
	}

	/**
	 * 保存日志级别
	 * @param loggerName loggerName
	 * @param level level
	 */
	public static void saveLogLevel(String loggerName, String level) {
		if("NULL".equalsIgnoreCase(level)){
			return;
		}
		String name = loggerName.substring(0, loggerName.indexOf("["));
		Logger logger = LogManager.exists(name);
		if(logger == null){
			return;
		}
		logger.setLevel(Level.toLevel(level)); 
	}

	/**
	 * 获取context正在执行的任务线程的堆栈
	 * @param name context名称
	 * @return map结果
	 */
	public static Map<String, Object> activeDetail(String name) {
		Map<String,Object> map = new HashMap<>();
		map.put("size", 0);
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return map;
		}

		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss SSS");
		for(Entry<Task<?>, Thread> entry : context.getActiveThreads().entrySet()){
			Task<?> task = entry.getKey();
			Thread thread = entry.getValue();

			Map<String, String> subMap = new HashMap<>();
			subMap.put("createTime", format.format(task.getCreateTime()));
			subMap.put("startTime", format.format(task.getStartTime()));

			StringBuilder builder = new StringBuilder();
			for(StackTraceElement stack : thread.getStackTrace()){
				builder.append(stack).append("<br>");
			}
			subMap.put("stack", builder.toString());
			map.put(task.id, subMap);
		}
		map.put("size", map.size() - 1);
		return map;
	}

	/**
	 * 获取失败的任务详情
	 * @param name context名称
	 * @return map结果
	 */
	public static Map<String, Object> failedDetail(String name) {
		Map<String,Object> map = new HashMap<>();
		map.put("size", 0);
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return map;
		} 

		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss SSS");
		for(Entry<String, Result<?>> entry : context.statistics.failedMap.entrySet()){
			Result<?> result = entry.getValue();

			Map<String, Object> subMap = new HashMap<>();
			subMap.put("createTime", format.format(result.getCreateTime()));
			subMap.put("startTime", format.format(result.getStartTime()));
			subMap.put("costTime", result.getCostTime());

			if(result.getThrowable() == null){
				subMap.put("throwable", "null");
			}else{
				Throwable throwable = result.getThrowable();
				StringBuilder builder = new StringBuilder("msg = " + throwable.getMessage() + "<br>stackTrace:<br>");
				for(StackTraceElement stack : throwable.getStackTrace()){
					builder.append(stack).append("<br>");
				}
				subMap.put("throwable", builder.toString());
			}
			map.put(entry.getKey(), subMap);
		}
		map.put("size", map.size() - 1);
		return map;
	}

	/**
	 * 获取正在等待的任务详情
	 * @param name context名称
	 * @return map结果
	 */
	public static Map<String, Object> waitingdetail(String name) {
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			Map<String,Object> map = new HashMap<>();
			map.put("size", 0);
			return map;
		} 
		Map<String,Object> map = context.getWaitingDetail();
		map.put("size", map.size());
		return map;
	}

	/**
	 * 获取成功的任务耗时详情
	 * @param name context名称
	 * @return map结果
	 * @throws Exception Exception
	 */
	public static Map<String, Object> successDetail(String name) throws Exception {
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return new HashMap<String, Object>(); 
		}
		return context.statistics.successDetail();
	}
	
	private static final int LEVELINDEX0 = 0;
	
	private static final int LEVELINDEX1 = 1;
	
	private static final int LEVELINDEX2 = 2;
	
	private static final int LEVELINDEX3 = 3;
	
	private static final int LEVELINDEX4 = 4;

	/**
	 * 保存耗时统计区间
	 * @param name name
	 * @param levelStr levelStr
	 * @param saveDay saveDay
	 * @param date date
	 * @return map
	 * @throws Exception Exception
	 */
	public static Map<String, Object> saveCostLevel(String name, String levelStr, String saveDay, String date) throws Exception {
		Map<String, Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("isSuccess", false);
			return map;
		} 

		map.put("isSuccess", true);
		int day = Integer.parseInt(saveDay);
		if(context.statistics.saveDay != day){
			//认为页面请求只是辅助，几乎不存在并发，所有尽量不使用同步
			context.statistics.saveDay = day; 
		}
		map.put("saveDay", context.statistics.saveDay);

		String[] array = levelStr.split(",");
		long v1 = Long.parseLong(array[LEVELINDEX0]);
		long v2 = Long.parseLong(array[LEVELINDEX1]);
		long v3 = Long.parseLong(array[LEVELINDEX2]);
		long v4 = Long.parseLong(array[LEVELINDEX3]);
		long v5 = Long.parseLong(array[LEVELINDEX4]);
		if(v1 >= v2 && v2 >= v3 && v3 >= v4 && v4 >= v5){
			map.put("isChange", false);
			return map;
		}

		if(!context.statistics.levelChange(v1, v2, v3, v4, v5)){
			map.put("isChange", false);
			return map;
		}

		map.put("isChange", true);
		context.statistics.dayDetail(map, date);
		return map;
	}

	/**
	 * 调整起止日期
	 * @param name name
	 * @param date date
	 * @return map
	 * @throws Exception Exception
	 */
	public static Map<String, Object> changeDate(String name, String date) throws Exception {
		Map<String, Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("isSuccess", false);
			return map;
		} 
		context.statistics.dayDetail(map, date);
		return map;
	}

	/**
	 * 获取成功任务明细,json形式
	 * @param name name
	 * @throws Exception Exception
	 */
	public static String dataDownload(String name) throws Exception {
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return "";
		} 

		StringBuilder builder = new StringBuilder("[");
		for(Entry<String, Queue<CostDetail>> entry : context.statistics.successMap.entrySet()){
			String date = entry.getKey();
			builder.append("{\"date\":\"").append(date + "\",\"details\":[");

			Queue<CostDetail> queue = entry.getValue();
			CostDetail[] array = new CostDetail[queue.size()];
			array = queue.toArray(array);
			for(int i = 0;i < array.length;i++){
				builder.append("{\"id\":\"").append(array[i].id).append("\",");
				builder.append("\"createTime\":\"").append(array[i].createTime).append("\",");
				builder.append("\"startTime\":\"").append(array[i].startTime).append("\",");
				builder.append("\"costTime\":\"").append(array[i].cost).append("\"}");
				if(i != array.length - 1){
					builder.append(",");
				}
			}
			builder.append("]},");
		}
		String json = builder.toString();
		json = json.substring(0, json.length() - 1) + "]";
		return json;
	}
	
	/**
	 * 获取最近一次异常信息
	 * @return map
	 */
	public static Map<String, String> getLastExceptions(String name){
		Map<String,String> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return map;
		} 
		
		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss SSS");
		for(Entry<Long, Exception> entry : context.lastException.entrySet()){
			String key = format.format(entry.getKey());
			Exception e = entry.getValue();
			StringBuilder builder = new StringBuilder("msg = " + e.getMessage() + "<br>stackTrace:<br>");
			for(StackTraceElement stack : e.getStackTrace()){
				builder.append(stack).append("<br>");
			}
			map.put(key, builder.toString());
		}
		return map;
	}
}
