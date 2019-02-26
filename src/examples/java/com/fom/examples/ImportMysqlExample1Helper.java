package com.fom.examples;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import com.fom.context.SpringContext;
import com.fom.context.helper.ParseHelper;
import com.fom.context.reader.Reader;
import com.fom.context.reader.TextReader;
import com.fom.examples.bean.ExampleBean;
import com.fom.examples.dao.ExamplesDao;

/**
 * 
 * @author shanhm
 *
 */
public class ImportMysqlExample1Helper implements ParseHelper<ExampleBean> {
	
	private final Logger log;

	public ImportMysqlExample1Helper(String name) {
		log = Logger.getLogger(name);
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	public void praseLineData(List<String> columns, List<ExampleBean> batchData, long batchTime) throws Exception {
		log.info("解析行数据:" + columns);
		ExampleBean bean = new ExampleBean(columns);
		bean.setSource("local");
		bean.setFileType("txt");
		bean.setImportWay("mybatis");
		batchData.add(bean); 
	}
	
	@Override
	public void batchProcessLineData(List<ExampleBean> lineDatas, long batchTime) throws Exception {
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
