package com.fom.examples;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.executor.helper.AbstractLocalZipImporterHelper;
import com.fom.context.executor.reader.Reader;
import com.fom.context.executor.reader.TextReader;
import com.fom.db.handler.JdbcHandler;

/**
 * 
 * @author shanhm
 * @date 2019年1月24日
 *
 */
public class Example5Helper extends AbstractLocalZipImporterHelper<Map<String, Object>> {

	private static final String POOL = "example_oracle";

	private static final String SQL = 
			"insert into demo(id,name,source,filetype,importway) "
					+ "values (#id#,#name#,#source#,#fileType#,#importWay#)";
	
	private Pattern pattern;

	public Example5Helper(String name, Pattern pattern) {
		super(name);
		this.pattern = pattern;
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
		map.put("id", array[0]);
		map.put("name", array[1]);
		map.put("source", "local");
		map.put("fileType", "zip(txt)");
		map.put("importWay", "pool");
		lineDatas.add(map);
	}

	@Override
	public void batchProcessIfNotInterrupted(List<Map<String, Object>> lineDatas, long batchTime) throws Exception {
		JdbcHandler.handler.batchExecute(POOL, SQL, lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
	}

	@Override
	public boolean delete(String sourceUri) {
		return new File(sourceUri).delete();
	}

	@Override
	public long getSourceSize(String sourceUri) {
		return new File(sourceUri).length();
	}

	@Override
	public boolean matchEntryName(String entryName) {
		if(pattern == null){
			return true;
		}
		//如果helper被共用，需要考虑线程安全
		synchronized (pattern) {
			return pattern.matcher(entryName).find();
		}
	}
}
