package com.fom.context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Service;

/**
 * 
 * @author shanhm
 *
 */
@Service(value="fomService")
public class FomServiceImpl implements FomService {

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
	public Map<String, Object> save(String name, String data) throws Exception {
		Map<String,Object> map = new HashMap<>();
		Context context = ContextManager.contextMap.get(name);
		if(context == null){
			map.put("result", false);
			map.put("msg", "context[" + name + "] not exist.");
			return map;
		} 
		//TODO
		
		//比较map
		//不同则序列化
		//返回值true则填充span和input
		return null;
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

}
