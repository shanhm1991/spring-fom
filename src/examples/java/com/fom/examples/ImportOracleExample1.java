package com.fom.examples;

import java.util.List;
import java.util.regex.Pattern;

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
@FomContext(remark="使用Mybatis的方式将本地指定目录下text文本的zip包解析导入Oracle库")
public class ImportOracleExample1 extends Context {

	private String srcPath = "${webapp.root}/source";

	private int batch = 5000;

	private boolean isDelMatchFail = false;
	
	private Pattern pattern;
	
	@Override
	protected List<String> getUriList() throws Exception {
		return ScanUtil.scan(srcPath, pattern, isDelMatchFail);
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		ImportOracleExample1Helper helper = 
				new ImportOracleExample1Helper(getName(), "pattern");
		LocalZipParser localZipParser = new LocalZipParser(sourceUri, batch, helper);
		return localZipParser;
	}
}
