package com.fom.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
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
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
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
			cmap.putAll(context.valueMap); 

			cmap.put("name", context.name);
			cmap.put("state", context.getState().name());
			if(!cmap.containsKey(Constants.CRON)){
				cmap.put(Constants.CRON, ""); 
			}
			cmap.put("execTime", format.format(context.execTime));
			cmap.put("loadTime", format.format(context.loadTime));
			cmap.put("level", context.log.getLevel().toString());
			cmap.put("active", String.valueOf(context.getActives())); 
			cmap.put("waiting", String.valueOf(context.getWaitings()));
			cmap.put("failed", context.failedDetail());
			String[] array = context.successDetail();
			cmap.put("success", array[0]);
			cmap.put("minCost", array[1]);
			cmap.put("maxCost", array[2]);
			cmap.put("avgCost", array[3]);
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
		bakMap.putAll(context.valueMap);

		JSONObject jsonObject = JSONObject.parseObject(json);  
		for(Entry<String, Object> entry : jsonObject.entrySet()){
			String key = entry.getKey();
			String value = String.valueOf(entry.getValue());
			switch(key){
			case Constants.QUEUESIZE:
				context.setQueueSize(Integer.parseInt(value)); break;
			case Constants.THREADCORE:
				context.setThreadCore(Integer.parseInt(value)); break;
			case Constants.THREADMAX:
				context.setThreadMax(Integer.parseInt(value)); break;
			case Constants.ALIVETIME:
				context.setAliveTime(Integer.parseInt(value)); break;
			case Constants.OVERTIME:
				context.setOverTime(Integer.parseInt(value)); break;
			case Constants.CANCELLABLE:
				context.setCancellable(Boolean.parseBoolean(value)); break;
			case Constants.CRON:
				context.setCron(value); break;
			default:
				context.setValue(key, value);
			}
		}

		if(context.valueMap.equals(bakMap)){ 
			map.put("result", false);
			map.put("msg", "context[" + name + "] has nothing changed.");
			return map;
		}
		map.put("result", true);//已经更新成功

		String cache = System.getProperty("cache.context");
		String[] array = new File(cache).list();
		if(!ArrayUtils.isEmpty(array)){//将已有的缓存文件移到history
			for(String fname : array){
				if(name.equals(fname.split("\\.")[0])){
					File source = new File(cache + File.separator + fname);
					File dest = new File(cache + File.separator + "history" + File.separator + fname);
					if(!source.renameTo(dest)){
						LOG.error("context[" + name + "]移动文件失败:" + fname);
						map.put("msg", "context[" + name + "] changed success, but failed when save update to cache");
						return map;
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
			map.put("msg", "context[" + name + "] changed success.");
			return map;
		}catch(Exception e){
			LOG.error("context[" + name + "] save update failed.");
			map.put("msg", "context[" + name + "] changed success, but failed when save update to cache");
			return map;
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
		resMap.put("msg", name + " created.");
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
	public Map<String, Object> getActiveDetail(String name) throws Exception {
		Map<String,Object> map = new HashMap<>();
		map.put("size", 0);
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return map;
		}

		for(Thread thread : context.getActiveThreads()){
			StringBuilder builder = new StringBuilder();
			for(StackTraceElement stack : thread.getStackTrace()){
				builder.append(stack).append("<br>");
			}
			map.put(thread.getName(), builder.toString());
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
		
		for(Entry<String, Object> entry : context.failedMap.entrySet()){
			Object obj = entry.getValue();
			if(obj instanceof Throwable) {
				Throwable throwable = (Throwable)obj;
				StringBuilder builder = new StringBuilder("msg = " + throwable.getMessage() + "<br>stackTrace:<br>");
				for(StackTraceElement stack : throwable.getStackTrace()){
					builder.append(stack).append("<br>");
				}
				map.put(entry.getKey(), builder.toString());
			}else{
				map.put(entry.getKey(), obj.toString());
			}
		}
		map.put("size", map.size() - 1);
		return map;
	}

	@Override
	public Map<String, Object> waitingdetail(String name) throws Exception {
		Map<String,Object> map = new HashMap<>();
		map.put("size", 0);
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return map;
		} 
		map = context.waitingDetail();
		map.put("size", map.size());
		return map;
	}

}
