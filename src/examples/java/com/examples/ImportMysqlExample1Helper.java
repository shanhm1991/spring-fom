package com.examples;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.examples.bean.ExampleBean;
import com.examples.dao.ExamplesDao;
import com.fom.context.SpringContext;
import com.fom.task.helper.TxtParseHelper;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.task.reader.TxtReader;

/**
 * 
 * @author shanhm
 *
 */
public class ImportMysqlExample1Helper implements TxtParseHelper<ExampleBean> {
	
	private final Logger log;

	public ImportMysqlExample1Helper(String name) {
		log = Logger.getLogger(name);
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TxtReader(sourceUri, "#");
	}

	@Override
	public List<ExampleBean> parseRowData(RowData rowData, long batchTime) throws Exception {
		ExampleBean bean = new ExampleBean(rowData.getColumnList());
		bean.setSource("local");
		bean.setFileType("txt");
		bean.setImportWay("mybatis");
		return Arrays.asList(bean);
	}
	
	@Override
	public void batchProcess(List<ExampleBean> lineDatas, long batchTime) throws Exception {
		ExamplesDao demoDao = SpringContext.getBean("mysqlExampleDao", ExamplesDao.class);
		demoDao.batchInsert(lineDatas);
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

}
