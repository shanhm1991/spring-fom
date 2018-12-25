package com.fom.modules.importer.demo;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.ZipImporter;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class DemoZipImporter extends ZipImporter<DemoZipImporterConfig, DemoZipBean>{

	protected DemoZipImporter(String name, String path) {
		super(name, path);
	}

	/**
	 * 继承自Executor，在任务线程启动时执行的第一个动作，可以完成一些准备操作
	 */
	@Override
	protected void onStart(DemoZipImporterConfig config) throws Exception {
		super.onStart(config);
	}

	/**
	 * 继承自ZipImporter，校验zip包含的文件是否合法
	 */
	@Override
	protected boolean validContents(DemoZipImporterConfig config, List<String> nameList) {
		return true;
	}

	/**
	 * [Abstract]继承自Importer, 将行数据line解析成DemoBean，并添加到lineDatas中去
	 * 异常则结束任务，保留文件，所以对错误数据导致的异常需要try-catch，一避免任务重复失败
	 */
	@Override
	protected void praseLineData(DemoZipImporterConfig config, List<DemoZipBean> lineDatas, String line, long batchTime)
			throws Exception {
		log.info("解析行数据:" + line);
		if(StringUtils.isBlank(line)){
			return;
		}
		lineDatas.add(new DemoZipBean(line)); 
	}

	/**
	 * [Abstract]继承自Importer, 批处理行数据解析结果, 异常则结束任务，保留文件
	 */
	@Override
	protected void batchProcessLineData(DemoZipImporterConfig config, List<DemoZipBean> lineDatas, long batchTime)
			throws Exception {
		log.info("处理数据入库:" + lineDatas.size());
		
		//1. OraHandler.handler.batchExecute(poolName, sql, paramMaps);
		
		//2.
		
	}

	/**
	 * 继承自Executor，在任务线程完成时执行的动作
	 */
	@Override
	protected void onComplete(DemoZipImporterConfig config) throws Exception {

	}
}
