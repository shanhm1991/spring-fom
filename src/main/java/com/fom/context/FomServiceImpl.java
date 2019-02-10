package com.fom.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
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
			Map<String,String> valueMap = context.valueMap;
			valueMap.put("name", context.name);
			valueMap.put("state", context.stateString());
			if(!valueMap.containsKey(Context.CRON)){
				valueMap.put(Context.CRON, "null"); 
			}
			valueMap.put("creatTime", format.format(context.createTime));
			valueMap.put("startTime", format.format(context.startTime));
			valueMap.put("level", context.log.getLevel().toString());
			list.add(valueMap);
		}
		map.put("data", list);
		map.put("length", list.size());
		map.put("draw", 1);
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
			case Context.QUEUESIZE:
				context.setQueueSize(Integer.parseInt(value)); break;
			case Context.THREADCORE:
				context.setThreadCore(Integer.parseInt(value)); break;
			case Context.THREADMAX:
				context.setThreadMax(Integer.parseInt(value)); break;
			case Context.ALIVETIME:
				context.setAliveTime(Integer.parseInt(value)); break;
			case Context.OVERTIME:
				context.setOverTime(Integer.parseInt(value)); break;
			case Context.CANCELLABLE:
				context.setCancellable(Boolean.parseBoolean(value)); break;
			case Context.CRON:
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
			LOG.error("context[" + name + "]保存更新失败");
			map.put("msg", "context[" + name + "] changed success, but failed when save update to cache");
			return map;
		}finally{
			IoUtil.close(out);
		}
	}

	@Override
	public Map<String,Object> start(String name) throws Exception {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		return context.start();
	}

	@Override
	public Map<String,Object> stop(String name){
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		return context.stop();
	}

	@Override
	public Map<String, Object> interrupt(String name) throws Exception {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		}
		return context.interrupt();
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
		map.put("state", context.stateString());
		return map;
	}

	@Override
	public void changeLogLevel(String name, String level) {
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			return;
		}
		context.changeLogLevel(level);
	}

}
