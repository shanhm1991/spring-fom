package com.examples.task.input;

import java.util.Arrays;
import java.util.List;

import org.eto.fom.boot.SpringContext;
import org.eto.fom.task.parse.ParseTextZipTask;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.reader.IReader;
import org.eto.fom.util.file.reader.IRow;
import org.eto.fom.util.file.reader.TextReader;

import com.examples.bean.ExampleBean;
import com.examples.dao.ExamplesDao;

/**
 * 
 * @author shanhm
 *
 */
public class InputOracleExample1Task extends ParseTextZipTask<ExampleBean> {
	
	private final String pattern;
	
	public InputOracleExample1Task(String sourceUri, int batch, String pattern) {
		super(sourceUri, batch); 
		this.pattern = pattern;
	}

	@Override
	public IReader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	public List<ExampleBean> parseRowData(IRow rowData, long batchTime) throws Exception {
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
