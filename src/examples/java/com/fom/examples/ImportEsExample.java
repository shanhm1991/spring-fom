package com.fom.examples;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.util.ScanUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="使用自定义pool的方式将本地指定目录下text文本解析导入Es库")
public class ImportEsExample extends Context {

	private String srcPath = "${webapp.root}/source";

	private int batch = 5000;

	private boolean isDelMatchFail = false;
	
	private Pattern pattern;

	private String esIndex = "demo";

	private String esType = "demo";

	private File esJsonFile = new File("WEB-INF/index/index_example.json"); 

	@Override
	protected List<String> getUriList() throws Exception {
		return ScanUtil.scan(srcPath, pattern, isDelMatchFail);
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		ImportEsExampleHelper helper = new ImportEsExampleHelper(getName(), esIndex, esType); 
		ImportEsExampleExecutor executor = 
				new ImportEsExampleExecutor(sourceUri, batch, helper, esIndex, esType,  esJsonFile);
		return executor;
	}
}
