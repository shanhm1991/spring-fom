package com.fom.examples;

import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.context.executor.LocalZipParser;
import com.fom.util.ScanUtil;

/**
 * 	
 * @author shanhm
 *
 */
@FomContext(names="ImportOracleExample1", remark="使用Mybatis的方式将本地指定目录下text文本的zip包解析导入Oracle库")
public class ImportOracleExample1 extends Context<TextZipImporterConfig>{

	@Override
	protected List<String> getUriList(TextZipImporterConfig config) throws Exception {
		return ScanUtil.scan(config.getSrcPath(), config.getPattern(), config.isDelMatchFail());
	}

	@Override
	protected Executor createExecutor(String sourceUri, TextZipImporterConfig config) throws Exception {
		ImportOracleExample1Helper helper = new ImportOracleExample1Helper(getName(), config.getEntryPattern());
		LocalZipParser localZipParser = new LocalZipParser(getName(), sourceUri, config.getBatch(), helper);
		return localZipParser;
	}
}
