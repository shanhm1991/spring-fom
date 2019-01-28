package com.fom.examples;

import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.Parser;
import com.fom.util.ScanUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(names="ImportMysqlExample2", remark="使用自定义pool的方式将本地指定目录下text文本解析导入Mysql库")
public class ImportMysqlExample2 extends Context<TextImporterConfig> {

	@Override
	protected List<String> getUriList(TextImporterConfig config) throws Exception {
		return ScanUtil.scan(config.getSrcPath(), config.getPattern(), config.isDelMatchFail());
	}

	@Override
	protected Executor createExecutor(String sourceUri, TextImporterConfig config) throws Exception {
		ImportMysqlExample2Helper helper = new ImportMysqlExample2Helper(getName());
		Parser parser = new Parser(getName(), sourceUri, sourceUri, config.getBatch(), helper);
		return parser;
	}
}
