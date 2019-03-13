package com.examples;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.examples.bean.ExampleBean;
import com.examples.dao.ExamplesDao;
import com.fom.context.SpringContext;
import com.fom.task.helper.TextZipParseHelper;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.task.reader.TxtReader;
import com.fom.util.PatternUtil;

/**
 * 
 * @author shanhm
 *
 */
public class ImportOracleExample1Helper implements TextZipParseHelper<ExampleBean> {
	
	private final String pattern;
	
	private final Logger log;

	public ImportOracleExample1Helper(String name, String pattern) {
		this.pattern = pattern;
		this.log = Logger.getLogger(name);
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TxtReader(sourceUri, "#");
	}

	@Override
	public List<ExampleBean> parseRowData(RowData rowData, long batchTime) throws Exception {
		ExampleBean bean = new ExampleBean(rowData.getColumnList());
		bean.setSource("local");
		bean.setFileType("zip(txt)");
		bean.setImportWay("mybatis");
		return Arrays.asList(bean);
	}
	
	@Override
	public void batchProcess(List<ExampleBean> lineDatas, long batchTime) throws Exception {
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
