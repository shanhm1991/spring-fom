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

		List<Map<String, Object>> list = new ArrayList<>();
		for(Entry<String, Context> entry : ContextManager.contextMap.entrySet()){
			Context context = entry.getValue();
			Map<String,Object> valueMap = context.valueMap;
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
	public String detail(String name){
		Context c = ContextManager.contextMap.get(name);
		Map<String,Object> map = c.valueMap;
		map.put("name", c.name);
		map.put("class", c.getClass().getName());
		return map.toString();
	}

	@Override
	public String xml(String name) throws Exception{
		//		Config config = ConfigManager.get(name);
		//		SAXReader reader = new SAXReader();
		//		StringReader in=new StringReader(config.getXml());  
		//		Document doc=reader.read(in); 
		//		OutputFormat formater=OutputFormat.createPrettyPrint();  
		//		formater.setEncoding("UTF-8");  
		//		StringWriter out=new StringWriter();  
		//		XMLWriter writer=new XMLWriter(out,formater);
		//		writer.write(doc);  
		//		writer.close();  
		//		return out.toString();
		return null;
	}

	@Override
	public String apply(String name, String data) throws Exception {
		//		SAXReader reader = new SAXReader();
		//		reader.setEncoding("UTF-8");
		//		StringReader in=new StringReader(data);  
		//		Document doc = reader.read(in); 
		//		Element element = doc.getRootElement();
		//		
		//		Config config = ConfigManager.load(element);
		//		if(!name.equals(config.name)){ 
		//			return "failed, [" + name + "]config.name can not be change.";
		//		}
		//		if(!config.valid){
		//			return "failed, [" + name + "]config not valid.";
		//		}
		//		
		//		Config oldConfig = ConfigManager.get(name);
		//		if(!config.getClass().getName().equals(oldConfig.getClass().getName())){
		//			return "failed, [" + name + "]config.class can not be change.";
		//		}
		//		if(config.equals(oldConfig)){ 
		//			return "failed, [" + name + "]config not changed.";
		//		}
		//
		//		File apply = new File(System.getProperty("config.apply"));
		//		for(File file : apply.listFiles()){
		//			if(file.getName().startsWith(name + ".xml.") && !file.delete()){
		//				throw new RuntimeException("删除文件失败:" + file.getName());
		//			}
		//		}
		//		File xml = new File(apply + File.separator + name + ".xml." + config.loadTime);
		//		XmlUtil.writeDocToFile(doc, xml);
		//		FileUtils.copyFile(xml, new File(apply + File.separator + "history" + File.separator + xml.getName()));
		//		
		//		ConfigManager.register(config); 
		//		return "success, " + name + " update applyed.";
		return null;
	}

	@Override
	public String stop(String name){
		//		Config config = ConfigManager.get(name);
		//		if(config == null){
		//			return "failed, " + name + " not exist.";
		//		}
		//		if(!config.valid){
		//			return "failed, " + name + " not valid.";
		//		}
		//		if(!config.isRunning){
		//			return "failed, " + name + " was not Running.";
		//		}
		//		config.isRunning = false;
		//		config.scanner.interrupt();
		//		return "success, " + name + " stoped.";
		return null;
	}

	@Override
	public String stopAll(){
		//		Map<String, Config> cmap = ConfigManager.getMap();
		//		StringBuilder builder = new StringBuilder();
		//		for(Entry<String, Config> entry : cmap.entrySet()){
		//			Config config = entry.getValue();
		//			if(!config.valid){
		//				builder.append("failed, " + config.name + " not valid.<br>");
		//				continue;
		//			}
		//			if(!config.isRunning){
		//				builder.append("failed, " + config.name + " was not Running.<br>");
		//				continue;
		//			}
		//			config.isRunning = false;
		//			config.scanner.interrupt();
		//			builder.append("success, " + config.name + " stoped.<br>");
		//		}
		//		return builder.toString();
		return null;
	}

	@Override
	public String start(String name) throws Exception {
		//		Config config = ConfigManager.get(name);
		//		if(config == null){
		//			return "failed, " + name + " not exist.";
		//		}
		//		if(config.isRunning){
		//			return "failed, " + name + " was already Running.";
		//		}
		//		if(config.scanner.isAlive()){
		//			return "failed, " + name + " was still alive, please try later.";
		//		}
		//		config.isRunning = true;
		//		config.refreshScanner();
		//		config.scanner.start();
		//		config.startTime = System.currentTimeMillis();
		//		return "success, " + name + " started.";
		return null;
	}

	@Override
	public String startAll() throws Exception {
		//		Map<String, Config> cmap = ConfigManager.getMap();
		//		StringBuilder builder = new StringBuilder();
		//		for(Entry<String, Config> entry : cmap.entrySet()){
		//			Config config = entry.getValue();
		//			if(config.isRunning){
		//				builder.append("failed, " + config.name + " was already Running.<br>");
		//				continue;
		//			}
		//			if(config.scanner.isAlive()){
		//				builder.append("failed, " + config.name + " was still alive, please try later.<br>");
		//				continue;
		//			}
		//			config.isRunning = true;
		//			config.refreshScanner();
		//			config.scanner.start();
		//			config.startTime = System.currentTimeMillis();
		//			builder.append("success, " + config.name + " started.<br>");
		//		}
		//		return builder.toString();
		return null;
	}

	@Override
	public String restart(String name) throws Exception {
		//		Config config = ConfigManager.get(name);
		//		if(config == null){
		//			return "failed, " + name + " not exist.";
		//		}
		//		if(!config.isRunning){
		//			return "failed, " + name + " was not Running.";
		//		}
		//		config.scanner.interrupt();
		//		config.startTime = System.currentTimeMillis();
		//		return "success, " + name + " restarted.";
		return null;
	}

	@Override
	public String restartAll() {
		//		Map<String, Config> cmap = ConfigManager.getMap();
		//		StringBuilder builder = new StringBuilder();
		//		for(Entry<String, Config> entry : cmap.entrySet()){
		//			Config config = entry.getValue();
		//			if(!config.isRunning){
		//				builder.append("failed, " + config.name + " was not Running.<br>");
		//				continue;
		//			}
		//			config.scanner.interrupt();
		//			config.startTime = System.currentTimeMillis();
		//			builder.append("success, " + config.name + " restarted.<br>");
		//		}
		//		return builder.toString();
		return null;
	}

}
