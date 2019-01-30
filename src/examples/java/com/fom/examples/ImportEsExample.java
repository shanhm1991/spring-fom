package com.fom.examples;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Executor;
import com.fom.util.FileUtil;

/**
 * 
 * @author shanhm
 *
 */
public class ImportEsExample extends Context {

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;
	
	private Pattern pattern;

	private String esIndex;

	private String esType;

	private File esJson;
	
	public ImportEsExample(String name){
		super(name);
		srcPath = ContextUtil.getContextPath(getString("srcPath", ""));
		String str = getString("pattern", "");
		if(!StringUtils.isBlank(str)){
			pattern = Pattern.compile(str);
		}
		batch = getInt("batch", 5000);
		isDelMatchFail = getBoolean("isDelMatchFail", false);
		esIndex = getString("esIndex", "");
		esType = getString("esType", "");
		esJson = new File(ContextUtil.getContextPath((getString("esJson", "")))); 
	}

	@Override
	protected List<String> getUriList() throws Exception {
		List<String> list = FileUtil.scan(srcPath, pattern, isDelMatchFail);
		return list;
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		ImportEsExampleHelper helper = new ImportEsExampleHelper(getName(), esIndex, esType); 
		ImportEsExampleExecutor executor = 
				new ImportEsExampleExecutor(sourceUri, batch, helper, esIndex, esType,  esJson);
		return executor;
	}
}
