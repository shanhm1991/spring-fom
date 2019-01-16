package com.fom.examples.importer.local.es;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.Importer;
import com.fom.context.db.handler.EsHandler;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public class LocalEsPoolImpoter extends Importer<LocalEsImporterConfig, Map<String,Object>> {
	
	private static final String POOL = "example_es";
	
	protected LocalEsPoolImpoter(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void onStart(LocalEsImporterConfig config) throws Exception {
		if(EsHandler.handler.synCreateIndex(
				POOL, config.getEsIndex(), config.getEsType(), config.getEsJsonFile())){
			log.info("创建ES索引[index=" + "demo" + ", type=" + "demo" + "]");
		}
	}

	/**
	 * 
	 * 将行数据line解析成DemoBean，并添加到lineDatas中去
	 * 异常则结束任务，保留文件，所以对错误数据导致的异常需要try-catch，一避免任务重复失败
	 */
	@Override
	public void praseLineData(LocalEsImporterConfig config, List<Map<String, Object>> lineDatas, String line,
			long batchTime) throws Exception {
		log.info("解析行数据:" + line);
		if(StringUtils.isBlank(line)){
			return;
		}
		String[] array = line.split("#"); 
		Map<String,Object> map = new HashMap<>();
		map.put("ID", array[0]);
		map.put("NAME", array[1]);
		map.put("SOURCE", "local");
		map.put("FILETYPE", "txt/orc");
		map.put("IMPORTWAY", "pool");
		lineDatas.add(map);
	}

	/**
	 * 批处理行数据解析结果, 异常则结束任务，保留文件
	 */
	@Override
	public void batchProcessLineData(LocalEsImporterConfig config, List<Map<String, Object>> lineDatas,
			long batchTime) throws Exception {
		Map<String,Map<String,Object>> map = new HashMap<>();
		for(Map<String, Object> m : lineDatas){
			map.put(String.valueOf(m.get("ID")), m);
		}
		EsHandler.handler.bulkInsert(POOL, config.getEsIndex(), config.getEsType(), map); 
	}

}
