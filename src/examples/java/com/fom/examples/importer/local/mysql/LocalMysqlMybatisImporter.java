package com.fom.examples.importer.local.mysql;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.Importer;
import com.fom.examples.importer.DemoBean;
import com.fom.examples.importer.dao.DemoDao;
import com.fom.util.SpringUtil;

/**
 * 解析文本文件将数据导入mysql，使用mybatis
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalMysqlMybatisImporter extends Importer<LocalImporterConfig, DemoBean> {

	protected LocalMysqlMybatisImporter(String name, String path) {
		super(name, path);
	}


	/**
	 * 
	 * 将行数据line解析成DemoBean，并添加到lineDatas中去
	 * 异常则结束任务，保留文件，所以对错误数据导致的异常需要try-catch，一避免任务重复失败
	 */
	@Override
	public void praseLineData(LocalImporterConfig config, List<DemoBean> lineDatas, String line, long batchTime) throws Exception {
		log.info("解析行数据:" + line);
		if(StringUtils.isBlank(line)){
			return;
		}
		DemoBean bean = new DemoBean(line);
		bean.setSource("local");
		bean.setFileType("txt/orc");
		bean.setImportWay("mybatis");
		lineDatas.add(bean); 
	}

	/**
	 * 批处理行数据解析结果, 异常则结束任务，保留文件
	 */
	@Override
	public void batchProcessLineData(LocalImporterConfig config, List<DemoBean> lineDatas, long batchTime) throws Exception {
		DemoDao demoDao = SpringUtil.getBean("mysqlDemoDao", DemoDao.class);
		demoDao.batchInsertDemo(lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
	}

}
