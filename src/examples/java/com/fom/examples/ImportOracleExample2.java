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
@FomContext(remark="使用自定义pool的方式将本地指定目录下text文本的zip包解析导入Oracle库")
public class ImportOracleExample2 extends Context{

	@Override
	protected List<String> getUriList() throws Exception {
//		return ScanUtil.scan(config.getSrcPath(), config.getPattern(), config.isDelMatchFail());
		
		System.out.println(123); 
		
		return null;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
//		ImportOracleExample2Helper helper = new ImportOracleExample2Helper(getName(), config.getEntryPattern());
//		LocalZipParser localZipParser = new LocalZipParser(getName(), sourceUri, config.getBatch(), helper);
//		return localZipParser;
		
		return null;
	}
}
