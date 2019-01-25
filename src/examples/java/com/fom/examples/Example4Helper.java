package com.fom.examples;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.executor.helper.AbstractLocalZipImporterHelper;
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
public class Example4Helper extends AbstractLocalZipImporterHelper<ExampleBean> {

	private Pattern pattern;

	public Example4Helper(String name, Pattern pattern) {
		super(name);
		this.pattern = pattern;
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
		ExamplesDao demoDao = SpringUtil.getBean("oracleExampleDao", ExamplesDao.class);
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

	@Override
	public boolean matchEntryName(String entryName) {
		if(pattern == null){
			return true;
		}
		return pattern.matcher(entryName).find();
	}
}
