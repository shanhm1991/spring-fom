package org.eto.fom.context.core;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.eto.fom.context.Loader;
import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.ContextStatistics.CostDetail;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.springframework.beans.factory.annotation.Value;

/**
 * context的操作api
 * 
 * @author shanhm
 *
 */
public class ContextHelper {

	private static final Logger LOG = Logger.getLogger(ContextHelper.class);

	/**
	 * 替换字符串中的系统变量
	 * @param val string
	 * @return string
	 */
	public static String replaceSystemProperty(String val) {
		String begin = "${";
		char   end  = '}';
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
					buffer.append(val.substring(i));
					return buffer.toString();
				}
			} else {
				buffer.append(val, i, j);
				k = val.indexOf(end, j);
				if(k == -1) {
					throw new IllegalArgumentException('"' 
							+ val + "\" has no closing brace. Opening brace at position " + j + '.');
				} else {
					j += 2;
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
	 */
	public static <E> TimedFuture<Result<E>> submitTask(String contextName, Task<E> task) {
		@SuppressWarnings("unchecked")
		Context<E> context = (Context<E>)ContextManager.loadedContext.get(contextName);
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
		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		List<Map<String, String>> list = new ArrayList<>();
		for(Entry<String, Context<?>> entry : ContextManager.loadedContext.entrySet()){
			Context<?> context = entry.getValue();
			TreeMap<String,String> cmap = new TreeMap<>(context.config.valueMap);

			cmap.put("name", context.name);
			cmap.put("state", context.getState().name().toLowerCase());
			if(!cmap.containsKey(ContextConfig.CONF_CRON)){
				cmap.put(ContextConfig.CONF_CRON, ""); 
			}

			String exec = "";
			if(context.lastTime > 0){
				exec = format.format(context.lastTime);
			}
			cmap.put("lastTime", exec);

			String next = "";
			if(context.nextTime > 0){
				next = format.format(context.nextTime);
			}
			cmap.put("nextTime", next);
			cmap.put("execTimes", String.valueOf(context.batchScheduls)); 

			cmap.put("loadTime", format.format(context.loadTime));
			cmap.put("level", context.getLogLevel());
			cmap.put("active", String.valueOf(context.getActives())); 
			cmap.put("waiting", String.valueOf(context.getWaitings()));
			cmap.put("failed", String.valueOf(context.statistics.getFailed()));
			cmap.put("failedDetails", String.valueOf(context.statistics.getfailedDetails()));
			cmap.put("success", String.valueOf(context.getSuccess()));

			// 不允许编辑的参数
			cmap.remove(ContextConfig.CONF_STOPWITHNOSCHEDULE);
			cmap.remove(ContextConfig.CONF_EXECONLOAN);
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
		Context<?> context = ContextManager.loadedContext.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		} 
		Map<String,String> bakMap = new HashMap<>(context.config.valueMap);

		json = json.replaceAll("\\\\", "\\\\\\\\");
		JSONObject jsonObject = new JSONObject(json);

		@SuppressWarnings("unchecked")
		Iterator<String> it = jsonObject.keys();
		while(it.hasNext()){
			String key = it.next();
			String value = jsonObject.getString(key);
			switch(key){
			case ContextConfig.CONF_QUEUESIZE:
				context.config.setQueueSize(Integer.parseInt(value)); 
				break;
			case ContextConfig.CONF_THREADCORE:
				context.config.setThreadCore(Integer.parseInt(value)); 
				break;
			case ContextConfig.CONF_THREADMAX:
				context.config.setThreadMax(Integer.parseInt(value)); 
				break;
			case ContextConfig.CONF_ALIVETIME:
				context.config.setAliveTime(Integer.parseInt(value)); 
				break;
			case ContextConfig.CONF_OVERTIME:
				context.config.setOverTime(Integer.parseInt(value)); 
				break;
			case ContextConfig.CONF_CANCELLABLE:
				context.config.setCancellable(Boolean.parseBoolean(value)); 
				break;
			case ContextConfig.CONF_CRON:
				context.config.setCron(value); 
				break;
			case ContextConfig.CONF_FIXEDRATE:
				context.config.setFixedRate(Integer.parseInt(value));
				break;
			case ContextConfig.CONF_FIXEDDELAY:
				context.config.setFixedDelay(Integer.parseInt(value));
				break;
			default:
				context.config.put(key, value);
			}
		}

		refreshField(context);

		map.put("result", true);//已经更新成功
		if(context.config.valueMap.equals(bakMap)){ 
			map.put("msg", "context[" + name + "] has nothing to chang.");
			return map;
		}

		if(serialize(name, context)){
			map.put("msg", "context[" + name + "] changed.");
		}else{
			map.put("msg", "context[" + name + "] changed, but save failed.");
		}
		return map;
	}

	private static void refreshField(Context context) throws Exception {
		Class<?> clazz = context.getClass();
		Object instance = context;
		if(StringUtils.isNotBlank(context.fom_schedul)){
			clazz = Class.forName(context.fom_schedul);
			instance = SpringContext.getBean(clazz);
		}else if(StringUtils.isNotBlank(context.fom_schedulbatch)){
			clazz = Class.forName(context.fom_schedulbatch);
			instance = SpringContext.getBean(clazz);
		}

		for(Field f : clazz.getDeclaredFields()){
			Value v = f.getAnnotation(Value.class);
			if(v != null){
				ContextManager.valueField(f, instance, v.value(), context, false);
			}
		}
	}

	private static boolean serialize(String name, Context<?> context) {
		String cache = System.getProperty("cache.context");
		File[] array = new File(cache).listFiles(File::isFile);

		if(array != null){//将已有的缓存文件移到history
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

		Gson gson = new GsonBuilder().addSerializationExclusionStrategy(
				new ExclusionStrategy() {
					@Override
					public boolean shouldSkipField(FieldAttributes field) {
						return field.getAnnotation(Expose.class) != null;
					}
					@Override
					public boolean shouldSkipClass(Class<?> arg0) {
						return false;
					}
				}).create();

		String file = cache + File.separator + name + "." + System.currentTimeMillis();


		try(FileOutputStream output = new FileOutputStream(file)){
			Map<String, Object> map = new HashMap<>(); //规避匿名类的序列化问题
			map.put("name", context.name);
			map.put("fom_context", context.fom_context);
			map.put("fom_schedul", context.fom_schedul);
			map.put("fom_schedulbatch", context.fom_schedulbatch);

			Map<String, Map<String, String>> config = new HashMap<>();
			config.put("valueMap", context.config.valueMap);
			map.put("config", config);
			IOUtils.write(gson.toJson(map), output, "UTF-8");
			return true;
		}catch(Exception e){
			LOG.error("context[" + name + "] save failed: " + e);
			return false;
		}
	}

	/**
	 * 启动
	 * @param name context名称
	 * @return map结果
	 */
	public static Map<String,Object> startup(String name) {
		Map<String,Object> map = new HashMap<>();
		Context<?> context = ContextManager.loadedContext.get(name);
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
		Context<?> context = ContextManager.loadedContext.get(name);
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
		Context<?> context = ContextManager.loadedContext.get(name);
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
		Context<?> context = ContextManager.loadedContext.get(name);
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
	@SuppressWarnings({ "unchecked" })
	public static Map<String, Object> create(String json) throws Exception {
		Map<String, Object> resMap = new HashMap<>();

		json = json.replaceAll("\\\\", "\\\\\\\\");
		JSONObject jsonObject = new JSONObject(json);

		ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
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

		Class<?> contextClass;
		try {
			contextClass = Class.forName(clazz);
			if(!Context.class.isAssignableFrom(contextClass)){
				resMap.put("result", false);
				resMap.put("msg", clazz + " is not a Context");
				return resMap;
			}
		}catch(Exception e){
			LOG.error(clazz + "load failed", e);
			resMap.put("result", false);
			resMap.put("msg", clazz + " load failed.");
			return resMap;
		}

		String name = map.get("name");
		if(StringUtils.isBlank(name)){
			FomContext fc = contextClass.getAnnotation(FomContext.class);
			if(fc != null && !StringUtils.isBlank(fc.name())){
				name = fc.name();
			}else{
				name = contextClass.getSimpleName();
			}
		}

		if(ContextManager.exist(name)){ //此处有线程安全问题
			LOG.warn("context[" + name + "] already exist, create canceled.");
			resMap.put("result", false);
			resMap.put("msg", "context[" + name + "] already exist, create canceled.");
			return resMap;
		}

		Context.localName.set(name); 
		ContextConfig.loadedConfig.putIfAbsent(name, map);
		Context<?> context = (Context<?>)contextClass.newInstance();

		context.regist(false);
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
		Context<?> context = ContextManager.loadedContext.get(name);
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
			if(ContextManager.loadedContext.containsKey(logger.getName())){  
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
					builder.append(((Appender)appenderEnumeration.nextElement()).getName()).append(",");
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
				builder.append(((Appender)appenderEnumeration.nextElement()).getName()).append(",");
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
			if(ContextManager.loadedContext.containsKey(logger.getName())){  
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
		Context<?> context = ContextManager.loadedContext.get(name);
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
		Context<?> context = ContextManager.loadedContext.get(name);
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
		Context<?> context = ContextManager.loadedContext.get(name);
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
		Context<?> context = ContextManager.loadedContext.get(name);
		if(context == null){
			return new HashMap<>();
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
		Context<?> context = ContextManager.loadedContext.get(name);
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
		Context<?> context = ContextManager.loadedContext.get(name);
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
	 */
	public static String dataDownload(String name) {
		Context<?> context = ContextManager.loadedContext.get(name);
		if(context == null){
			return "";
		} 

		StringBuilder builder = new StringBuilder("[");
		for(Entry<String, Queue<CostDetail>> entry : context.statistics.successMap.entrySet()){
			String date = entry.getKey();
			builder.append("{\"date\":\"").append(date).append("\",\"details\":[");

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
}
