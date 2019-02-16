package com.fom.examples;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.fom.context.SpringContext;
import com.fom.context.helper.ZipParseHelper;
import com.fom.context.reader.Reader;
import com.fom.context.reader.TextReader;
import com.fom.examples.bean.ExampleBean;
import com.fom.examples.dao.ExamplesDao;
import com.fom.util.PatternUtil;

/**
 * 
 * @author shanhm
 *
 */
public class ImportOracleExample1Helper implements ZipParseHelper<ExampleBean> {
	
	private final String pattern;
	
	private final Logger log;

	public ImportOracleExample1Helper(String name, String pattern) {
		this.pattern = pattern;
		this.log = Logger.getLogger(name);
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri);
	}

	@Override
	public void praseLineData(List<ExampleBean> lineDatas, String line, long batchTime) throws Exception {
		log.info("解析行数据:" + line);
		if(StringUtils.isBlank(line)){
			return;
		}
		ExampleBean bean = new ExampleBean(line);
		bean.setSource("local");
		bean.setFileType("zip(txt)");
		bean.setImportWay("mybatis");
		lineDatas.add(bean); 
	}
	
	@Override
	public void batchProcessLineData(List<ExampleBean> lineDatas, long batchTime) throws Exception {
		ExamplesDao demoDao = SpringContext.getBean("oracleExampleDao", ExamplesDao.class);
		demoDao.batchInsert(lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
		
	}

	@Override
	public boolean matchEntryName(String entryName) {
		return PatternUtil.match(pattern, entryName);
	}

	@Override
	public boolean delete(String sourceUri) {
		return new File(sourceUri).delete();
	}

	@Override
	public long getSourceSize(String sourceUri) {
		return new File(sourceUri).length();
	}
	
}
