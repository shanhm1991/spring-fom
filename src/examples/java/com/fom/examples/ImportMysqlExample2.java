package com.fom.examples;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Executor;
import com.fom.context.executor.Parser;
import com.fom.util.FileUtil;

/**
 * 
 * @author shanhm
 *
 */
public class ImportMysqlExample2 extends Context {
	
	private static final long serialVersionUID = 3141790485417453373L;

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;
	
	private Pattern pattern;
	
	public ImportMysqlExample2(String name){
		super(name);
		srcPath = ContextUtil.getContextPath(getString("srcPath", ""));
		batch = getInt("batch", 5000);
		isDelMatchFail = getBoolean("isDelMatchFail", false);
		String str = getString("pattern", "");
		if(!StringUtils.isBlank(str)){
			pattern = Pattern.compile(str);
		}
	}

	@Override
	protected List<String> getUriList() throws Exception {
		return FileUtil.scan(srcPath, pattern, isDelMatchFail);
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		ImportMysqlExample2Helper helper = new ImportMysqlExample2Helper(getName());
		Parser parser = new Parser(sourceUri, batch, helper);
		return parser;
	}
}
