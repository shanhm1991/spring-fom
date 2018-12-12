package com.fom.modules.importer.demo.ml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fom.context.ZipImporter;
import com.fom.context.ZipImporterConfig;
import com.fom.util.db.handler.OraHandler;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
public class LabelDetailImporter extends ZipImporter<ZipImporterConfig, Map<String, Object>>{
	
	private static final String POOL_ORA = "scloudrs";

	private static final String SQL = "insert into NCRS_SUSPICIOUS_ORIGINDATA"
			+ "(apptype, msgid, senduserid, CAPTURETIME, sendip, sendipid, content, labelid, RECEIVERID, inserttime) "
			+ "values(#apptype#, #msgid#, #senduserid#, #CAPTURETIME#, #sendip#, #sendipid#, #content#, #labelid#, #RECEIVERID#, #inserttime#)";

	protected LabelDetailImporter(String name, String path) {
		super(name, path);
	}

	@Override
	protected void praseLineData(ZipImporterConfig config, List<Map<String, Object>> lineDatas, 
			String line, long batchTime) throws Exception {
		String[] array = line.trim().split("\t");
		if(array.length != 18){
			log.warn("忽略行数据,字段数不是18");
			return;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		try{
			map.put("CAPTURETIME", Long.valueOf(array[5]));
			map.put("sendip", Long.valueOf(array[6]));
		}catch(Exception e){
			log.warn("忽略行数据", e); 
			return;
		}
		map.put("apptype", array[0]);
		map.put("msgid", array[1]);
		map.put("senduserid", array[3]);
		map.put("sendipid", array[7]);
		map.put("content", array[10]);
		map.put("labelid", array[17]);
		map.put("RECEIVERID", array[2]);
		map.put("inserttime", batchTime / 1000);
		lineDatas.add(map);
	}

	@Override
	protected void batchProcessLineData(ZipImporterConfig config, List<Map<String, Object>> lineDatas, long batchTime) throws Exception {
		OraHandler.defaultHandler.batchExecute(POOL_ORA, SQL, lineDatas);
		log.info("批处理结束[" + lineDatas.size() + "], 耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
	}
}