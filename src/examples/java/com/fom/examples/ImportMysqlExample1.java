package com.fom.examples;

import java.util.List;
import java.util.regex.Pattern;

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
@FomContext(remark="使用Mybatis的方式将本地指定目录下text文本解析导入Mysql库")
public class ImportMysqlExample1 extends Context {
	
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
		ImportMysqlExample1Helper helper = new ImportMysqlExample1Helper(getName());
		Parser parser = new Parser(sourceUri, batch, helper);
		return parser;
	}
}
