package com.fom.examples;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.executor.helper.abstractImporterHelper;
import com.fom.context.executor.reader.Reader;
import com.fom.context.executor.reader.TextReader;
import com.fom.examples.bean.ExampleBean;
import com.fom.examples.dao.ExamplesDao;
import com.fom.util.SpringUtil;

/**
 * 
 * @author shanhm
 * @date 2019年1月24日
 *
 */
public class Example4Helper extends abstractImporterHelper<ExampleBean> {

	public Example4Helper(String name) {
		super(name);
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
	public void batchProcessIfNotInterrupted(List<ExampleBean> lineDatas, long batchTime) throws Exception {
		ExamplesDao demoDao = SpringUtil.getBean("oracleDemoDao", ExamplesDao.class);
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
