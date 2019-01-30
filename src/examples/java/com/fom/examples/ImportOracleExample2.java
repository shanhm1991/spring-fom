package com.fom.examples;

import java.util.List;
import java.util.regex.Pattern;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.executor.LocalZipParser;
import com.fom.util.FileUtil;

/**
 * 
 * @author shanhm
 *
 */
public class ImportOracleExample2 extends Context{

	private String srcPath = "${webapp.root}/source";

	private int batch = 5000;

	private boolean isDelMatchFail = false;

	private Pattern pattern;

	@Override
	protected List<String> getUriList() throws Exception {
		return FileUtil.scan(srcPath, pattern, isDelMatchFail);
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		ImportOracleExample2Helper helper = 
				new ImportOracleExample2Helper(getName(), "pattern");
		LocalZipParser localZipParser = new LocalZipParser(sourceUri, batch, helper);
		return localZipParser;
	}
}
