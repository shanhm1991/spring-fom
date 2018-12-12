package com.fom.modules.importer.demo.tm;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fom.context.ZipImporter;
import com.fom.context.ZipImporterConfig;
import com.fom.util.db.handler.OraHandler;

/**
 * 标签聚合
 */
public class TMImpoter extends ZipImporter<ZipImporterConfig, Map<String, Object>>{
	
	private static final String POOL = "scloudrs";

	private String sql;

	protected TMImpoter(String name, String path) throws SQLException {
		super(name, path);

		String col_TAG = "TAG_" + srcName.split("_")[0];
		StringBuilder build = new StringBuilder();
		if(srcName.contains("GROUP")){
			build.append("merge into TAG_WXGROUP using (select #id# as id, #name# as name, #time# as time from dual) t on (TAG_WXGROUP.GROUPID=t.id) ");
			build.append("when not matched then ");
			build.append("insert(GROUPID,INSERT_TIME,UPDATE_TIME,").append(col_TAG).append(") values (t.id,t.time,t.time,1)");
			build.append("when matched then update set ");
			build.append("TAG_WXGROUP.UPDATE_TIME=t.time,");
			build.append("TAG_WXGROUP.").append(col_TAG).append("=1");

		}else if(srcName.contains("USER")){
			build.append("merge into TAG_WXUSER using (select #id# as id, #name# as name, #time# as time from dual) t on (TAG_WXUSER.USERID=t.id) ");
			build.append("when not matched then ");
			build.append("insert(USERID,ACCOUNTNAME,INSERT_TIME,UPDATE_TIME,").append(col_TAG).append(") values (t.id,t.name,t.time,t.time,1)");
			build.append("when matched then update set ");
			build.append("TAG_WXUSER.UPDATE_TIME=t.time,");
			build.append("TAG_WXUSER.").append(col_TAG).append("=1");
		}
		sql = build.toString();
	}

	@Override
	protected void praseLineData(ZipImporterConfig config, List<Map<String, Object>> lineDatas, String line, long batchTime) {
		String[] splits = line.trim().split("\t"); 
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("time", batchTime / 1000);
		try{
			map.put("id", Long.parseLong(splits[0].trim()));
			map.put("name", splits[1]);
		}catch(Exception e){
			log.warn("忽略行数据", e);
		}
		lineDatas.add(map);
	}

	@Override
	protected void batchProcessLineData(ZipImporterConfig config, List<Map<String, Object>> lineDatas, long batchTime) throws Exception {
		OraHandler.defaultHandler.batchExecute(POOL, sql, lineDatas);
		log.info("批处理结束[" + lineDatas.size() + "], 耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
	}
}
