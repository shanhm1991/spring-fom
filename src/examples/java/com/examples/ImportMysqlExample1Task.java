package com.examples;

import java.util.Arrays;
import java.util.List;

import com.examples.bean.ExampleBean;
import com.examples.dao.ExamplesDao;
import com.fom.context.SpringContext;
import com.fom.task.TextParseTask;
import com.fom.task.reader.Reader;
import com.fom.task.reader.RowData;
import com.fom.task.reader.TextReader;

/**
 * 
 * @author shanhm
 *
 */
public class ImportMysqlExample1Task extends TextParseTask<ExampleBean> {
	
	public ImportMysqlExample1Task(String sourceUri, int batch){
		super(sourceUri, batch); 
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
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

}
