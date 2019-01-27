package com.fom.examples;

import com.fom.context.Context;
import com.fom.context.executor.Parser;

/**
 * 解析文本文件将数据导入mysql，使用mybatis
 * 
 * @author shanhm
 *
 */
public class Example2 extends Context<TextImporterConfig> {

	protected Example2(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected void exec(TextImporterConfig config) throws Exception {
		Example2Helper helper = new Example2Helper(name);
		Parser importer = new Parser(name, sourceUri, config.getBatch(), helper);
		importer.exec();
	}

}
