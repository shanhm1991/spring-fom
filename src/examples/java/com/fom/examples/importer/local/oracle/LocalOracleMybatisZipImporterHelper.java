package com.fom.examples.importer.local.oracle;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.executor.helper.abstractImporterHelper;
import com.fom.context.executor.reader.Reader;
import com.fom.context.executor.reader.TextReader;
import com.fom.examples.importer.DemoBean;
import com.fom.examples.importer.dao.DemoDao;
import com.fom.util.SpringUtil;

/**
 * 
 * @author shanhm
 * @date 2019年1月24日
 *
 */
public class LocalOracleMybatisZipImporterHelper extends abstractImporterHelper<DemoBean> {

	public LocalOracleMybatisZipImporterHelper(String name) {
		super(name);
	}

	@Override
	public Reader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri);
	}

	@Override
	public void praseLineData(List<DemoBean> lineDatas, String line, long batchTime) throws Exception {
		log.info("解析行数据:" + line);
		if(StringUtils.isBlank(line)){
			return;
		}
		DemoBean bean = new DemoBean(line);
		bean.setSource("local");
		bean.setFileType("zip(txt)");
		bean.setImportWay("mybatis");
		lineDatas.add(bean); 
	}
	
	@Override
	public void batchProcessIfNotInterrupted(List<DemoBean> lineDatas, long batchTime) throws Exception {
		DemoDao demoDao = SpringUtil.getBean("oracleDemoDao", DemoDao.class);
		demoDao.batchInsertDemo(lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
	}

	@Override
	public boolean delete(String sourceUri) {
		return new File(sourceUri).delete();
	}

	@Override
	public long getFileSize(String sourceUri) {
		return new File(sourceUri).length();
	}

	@Override
	public String getFileName(String sourceUri) {
		return new File(sourceUri).getName();
	}
}
