package com.examples;

import java.util.Arrays;
import java.util.List;

import com.examples.bean.ExampleBean;
import com.examples.dao.ExamplesDao;
import com.fom.context.SpringContext;
import com.fom.task.ParseTextZipTask;
import com.fom.task.reader.Reader;
import com.fom.task.reader.ReaderRow;
import com.fom.task.reader.TextReader;
import com.fom.util.PatternUtil;

/**
 * 
 * @author shanhm
 *
 */
public class ImportOracleExample1Task extends ParseTextZipTask<ExampleBean> {
	
	private final String pattern;
	
	public ImportOracleExample1Task(String sourceUri, int batch, String pattern) {
		super(sourceUri, batch); 
		this.pattern = pattern;
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	public List<ExampleBean> parseRowData(ReaderRow rowData, long batchTime) throws Exception {
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
	
}
