package com.fom.examples;

import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="使用自定义pool的方式将本地指定目录下text文本解析导入Mysql库")
public class ImportMysqlExample2 extends Context {

	@Override
	protected List<String> getUriList() throws Exception {
//		return ScanUtil.scan(config.getSrcPath(), config.getPattern(), config.isDelMatchFail());
		return null;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
//		ImportMysqlExample2Helper helper = new ImportMysqlExample2Helper(getName());
//		Parser parser = new Parser(getName(), sourceUri, sourceUri, config.getBatch(), helper);
//		return parser;
		return null;
	}
}
