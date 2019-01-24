package com.fom.examples.importer.local.mysql;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.executor.Importer;

/**
 * 解析文本文件将数据导入mysql，使用mybatis
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class LocalMysqlMybatisImporter extends Context<LocalTextImporterConfig> {

	protected LocalMysqlMybatisImporter(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void exec(LocalTextImporterConfig config) throws Exception {
		LocalMysqlMybatisImporterHelper helper = new LocalMysqlMybatisImporterHelper(name);
		Executor executor = new Importer(name, sourceUri, config, helper);
		executor.exec();
	}

}
