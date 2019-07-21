package com.examples.task.input;

import java.util.Arrays;
import java.util.List;

import org.eto.fom.boot.SpringContext;
import org.eto.fom.task.parse.ParseTextTask;
import org.eto.fom.util.file.reader.Reader;
import org.eto.fom.util.file.reader.ReaderRow;
import org.eto.fom.util.file.reader.TextReader;

import com.examples.bean.ExampleBean;
import com.examples.dao.ExamplesDao;

/**
 * 
 * @author shanhm
 *
 */
public class InputMysqlExample1Task extends ParseTextTask<ExampleBean> {
	
	public InputMysqlExample1Task(String sourceUri, int batch){
		super(sourceUri, batch); 
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	public List<ExampleBean> parseRowData(ReaderRow rowData, long batchTime) throws Exception {
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
