package com.fom.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.fom.context.ContextStatistics.CostDetail;
import com.fom.util.IoUtil;

/**
 * 
 * @author shanhm
 *
 */
@Service(value="fomService")
public class FomServiceImpl implements FomService {

	private static final Logger LOG = Logger.getLogger(FomServiceImpl.class);

	@Override
	public Map<String, Object> list() {
		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss SSS");
		Map<String, Object> map = new HashMap<>();

		List<Map<String, String>> list = new ArrayList<>();
		for(Entry<String, Context> entry : ContextManager.contextMap.entrySet()){
			Context context = entry.getValue();
			Map<String,String> cmap = new HashMap<>();
			cmap.putAll(context.config.valueMap); 

			cmap.put("name", context.name);
			cmap.put("state", context.getState().name());
			if(!cmap.containsKey(ContextConfig.CRON)){
				cmap.put(ContextConfig.CRON, ""); 
			}
			cmap.put("execTime", format.format(context.execTime));
			cmap.put("loadTime", format.format(context.loadTime));
			cmap.put("level", context.log.getLevel().toString());
			cmap.put("active", String.valueOf(context.getActives())); 
			cmap.put("waiting", String.valueOf(context.getWaitings()));
			cmap.put("failed", context.statistics.failedDetail());
			cmap.put("success", String.valueOf(context.getSuccess()));
			list.add(cmap);
		}
		map.put("data", list);
		map.put("length", list.size());
		map.put("recordsTotal", list.size());
		map.put("recordsFiltered", list.size());
		return map;
	}

	@Override
	public Map<String, Object> save(String name, String json) throws Exception {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		} 
		Map<String,String> bakMap = new HashMap<>();
		bakMap.putAll(context.config.valueMap);

		JSONObject jsonObject = JSONObject.parseObject(json);  
		for(Entry<String, Object> entry : jsonObject.entrySet()){
			String key = entry.getKey();
			String value = String.valueOf(entry.getValue());
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
	
	private boolean serialize(String name, Context context){
		String cache = System.getProperty("cache.context");
		String[] array = new File(cache).list();
		if(!ArrayUtils.isEmpty(array)){//将已有的缓存文件移到history
			for(String fname : array){
				if(name.equals(fname.substring(0, fname.lastIndexOf(".")))){
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

	@Override
	public Map<String,Object> startup(String name) throws Exception {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		return context.startup();
	}

	@Override
	public Map<String,Object> shutDown(String name){
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		return context.shutDown();
	}

	@Override
	public Map<String, Object> execNow(String name) throws Exception {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		return context.execNow();
	}

	@Override
	public Map<String, Object> state(String name) throws Exception {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		map.put("result", true);
		map.put("state", context.getState().name());
		return map;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map<String, Object> create(String json) throws Exception {
		Map<String, Object> resMap = new HashMap<>();

		Map<String,String> map = (Map<String,String>) JSONObject.parse(json);
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

	@Override
	public void changeLogLevel(String name, String level) {
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return;
		}
		context.changeLogLevel(level);
	}

	@SuppressWarnings("rawtypes")
	public Map<String, String> listOtherLogs() throws Exception {
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
	private void listLog(Category logger, Map<String, String> map, Set<String> listed){
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
				String append = "";
				while(appenderEnumeration.hasMoreElements()){
					append += ((Appender)appenderEnumeration.nextElement()).getName() + ",";
				}
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
			String append = "";
			while(appenderEnumeration.hasMoreElements()){
				append += ((Appender)appenderEnumeration.nextElement()).getName() + ",";
			}
			append = append.substring(0, append.length() - 1);
			Level level = logger.getLevel();

			map.put(loggerName + "[" + append + "]", level.toString());
			listed.add(loggerName);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public String queryLevel(String key) {
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
		String level = map.get(key);
		if(level == null){
			level = "NULL";
		}
		return level;
	}

	@Override
	public void saveLevel(String key, String level) {
		if("NULL".equalsIgnoreCase(level)){
			return;
		}
		String name = key.substring(0, key.indexOf("["));
		Logger logger = LogManager.exists(name);
		if(logger == null){
			return;
		}
		logger.setLevel(Level.toLevel(level)); 
	}

	@Override
	public Map<String, Object> activeDetail(String name) throws Exception {
		Map<String,Object> map = new HashMap<>();
		map.put("size", 0);
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return map;
		}

		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss SSS");
		for(Entry<Task, Thread> entry : context.getActiveThreads().entrySet()){
			Task task = entry.getKey();
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

	@Override
	public Map<String, Object> failedDetail(String name) throws Exception {
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

	@Override
	public Map<String, Object> waitingdetail(String name) throws Exception {
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

	@Override
	public Map<String, Object> successDetail(String name) throws Exception {
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return new HashMap<String, Object>(); 
		}
		return context.statistics.successDetail();
	}

	@Override
	public Map<String, Object> saveCostLevel(String name, String levelStr, String saveDay, String date) throws Exception {
		Map<String, Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("isSuccess", false);
			return map;
		} 

		map.put("isSuccess", true);
		int day = Integer.parseInt(saveDay);
		if(day >= 10){
			//认为页面请求只是辅助，几乎不存在并发，所有尽量不使用同步
			context.statistics.saveDay = day; 
		}
		map.put("saveDay", context.statistics.saveDay);

		String[] array = levelStr.split(",");
		long v1 = Long.parseLong(array[0]);
		long v2 = Long.parseLong(array[1]);
		long v3 = Long.parseLong(array[2]);
		long v4 = Long.parseLong(array[3]);
		long v5 = Long.parseLong(array[4]);
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

	@Override
	public Map<String, Object> changeDate(String name, String date) throws Exception {
		Map<String, Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("isSuccess", false);
			return map;
		} 
		context.statistics.dayDetail(map, date);
		return map;
	}

	@Override
	public void dataDownload(String name, HttpServletResponse resp) throws Exception {
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return;
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
		
		resp.reset();
		resp.setContentType("application/octet-stream;charset=UTF-8");
		resp.addHeader("Content-Disposition", "attachment;filename=\"" + name + "." + System.currentTimeMillis() +".json\"");
		PrintWriter write = resp.getWriter();
		write.write(json);
		write.flush();
	}
	
	public TimedFuture<Result<?>> submitTask(String contextName, Task task) throws Exception { 
		Context context = ContextManager.contextMap.get(contextName);
		if(context == null){
			throw new IllegalArgumentException("context[" + contextName + "] not exist.");
		} 
		return context.submit(task);
	}
	
}
