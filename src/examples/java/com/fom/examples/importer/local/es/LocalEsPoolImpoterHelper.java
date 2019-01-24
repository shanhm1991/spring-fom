package com.fom.examples.importer.local.es;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.executor.helper.importer.abstractImporterHelper;
import com.fom.context.executor.reader.Reader;
import com.fom.context.executor.reader.TextReader;
import com.fom.db.handler.EsHandler;

/**
 * 
 * @author shanhm
 * @date 2019年1月24日
 *
 */
public class LocalEsPoolImpoterHelper extends abstractImporterHelper<Map<String, Object>> {
	
	private static final String POOL = "example_es";
	
	private final String esIndex;
	
	private final String esType;

	public LocalEsPoolImpoterHelper(String name, String esIndex, String esType) {
		super(name);
		this.esIndex = esIndex;
		this.esType = esType;
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri);
	}

	@Override
	public void praseLineData(List<Map<String, Object>> lineDatas, String line, long batchTime) throws Exception {
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
	
	@Override
	public void batchProcessIfNotInterrupted(List<Map<String, Object>> lineDatas, long batchTime) throws Exception {
		Map<String,Map<String,Object>> map = new HashMap<>();
		for(Map<String, Object> m : lineDatas){
			map.put(String.valueOf(m.get("ID")), m);
		}
		EsHandler.handler.bulkInsert(POOL, esIndex, esType, map); 
	}

	@Override
	public boolean delete(String sourceUri) {
		return new File(sourceUri).delete();
	}

	@Override
	public long getFileSize(String sourceUri) {
		return new File(sourceUri).length();
	}

	@Override
	public String getFileName(String sourceUri) {
		return new File(sourceUri).getName();
	}


}
