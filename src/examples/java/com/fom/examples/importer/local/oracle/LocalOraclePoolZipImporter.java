package com.fom.examples.importer.local.oracle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.ZipImporter;
import com.fom.context.db.handler.JdbcHandler;

/**
 * 解析zip文件将数据导入oracle，使用自带pool
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalOraclePoolZipImporter extends ZipImporter<LocalZipImporterConfig, Map<String,Object>>{

	private static final String POOL = "example_oracle";
	
	private static final String SQL = 
			"insert into demo(id,name,source,filetype,importway) "
					+ "values (#id#,#name#,#source#,#fileType#,#importWay#)";
	
	protected LocalOraclePoolZipImporter(String name, String path) {
		super(name, path);
	}

	/**
	 * 将行数据line解析成DemoBean，并添加到lineDatas中去
	 * 异常则结束任务，保留文件，所以对错误数据导致的异常需要try-catch，一避免任务重复失败
	 */
	@Override
	public void praseLineData(LocalZipImporterConfig config, List<Map<String,Object>> lineDatas, String line, long batchTime)
			throws Exception {
		log.info("解析行数据:" + line);
		if(StringUtils.isBlank(line)){
			return;
		}
		String[] array = line.split("#"); 
		Map<String,Object> map = new HashMap<>();
		map.put("id", array[0]);
		map.put("name", array[1]);
		map.put("source", "local");
		map.put("fileType", "zip(txt/orc)");
		map.put("importWay", "pool");
		lineDatas.add(map);
	}

	/**
	 * 批处理行数据解析结果, 异常则结束任务，保留文件
	 */
	@Override
	public void batchProcessLineData(LocalZipImporterConfig config, List<Map<String,Object>> lineDatas, long batchTime)
			throws Exception {
		JdbcHandler.handler.batchExecute(POOL, SQL, lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
	}

}
