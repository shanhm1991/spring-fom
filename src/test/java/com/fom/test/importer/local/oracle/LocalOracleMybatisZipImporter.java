package com.fom.test.importer.local.oracle;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.ZipImporter;
import com.fom.test.importer.DemoBean;
import com.fom.test.importer.dao.DemoDao;
import com.fom.util.SpringUtil;

/**
 * 解析zip文件将数据导入oracle，使用mybatis
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalOracleMybatisZipImporter extends ZipImporter<LocalZipImporterConfig, DemoBean>{

	protected LocalOracleMybatisZipImporter(String name, String path) {
		super(name, path);
	}

	/**
	 * 继承自Executor，在任务线程启动时执行的第一个动作，可以完成一些准备操作
	 */
	@Override
	protected void onStart(LocalZipImporterConfig config) throws Exception {
		log.info("start process.");
	}

	/**
	 * 继承自ZipImporter，校验zip包含的文件是否合法
	 */
	@Override
	protected boolean validContents(LocalZipImporterConfig config, List<String> nameList) {
		log.info("zip contents valid true.");
		return true;
	}

	/**
	 * [Abstract]继承自Importer, 将行数据line解析成DemoBean，并添加到lineDatas中去
	 * 异常则结束任务，保留文件，所以对错误数据导致的异常需要try-catch，一避免任务重复失败
	 */
	@Override
	protected void praseLineData(LocalZipImporterConfig config, List<DemoBean> lineDatas, String line, long batchTime)
			throws Exception {
		log.info("解析行数据:" + line);
		if(StringUtils.isBlank(line)){
			return;
		}
		DemoBean bean = new DemoBean(line);
		bean.setSource("local");
		bean.setFileType("zip(txt/orc)");
		bean.setImportWay("mybatis");
		lineDatas.add(bean); 
	}

	/**
	 * [Abstract]继承自Importer, 批处理行数据解析结果, 异常则结束任务，保留文件
	 */
	@Override
	protected void batchProcessLineData(LocalZipImporterConfig config, List<DemoBean> lineDatas, long batchTime)
			throws Exception {
		DemoDao demoDao = SpringUtil.getBeanById("oracleDemoDao", DemoDao.class);
		demoDao.batchInsertDemo(lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
	}

	/**
	 * 继承自Executor，在任务线程完成时执行的动作
	 */
	@Override
	protected void onComplete(LocalZipImporterConfig config) throws Exception {
		log.info("complete process.");
	}
}
