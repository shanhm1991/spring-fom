package com.fom.examples;

import java.util.List;

import com.fom.context.Context;
import com.fom.context.Executor;
import com.fom.context.FomContext;
import com.fom.util.ScanUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(names="ImpotEsExample", remark="使用自定义pool的方式将本地指定目录下text文本解析导入Es库")
public class ImportEsExample extends Context<ImportEsExampleConfig> {
	
	@Override
	protected List<String> getUriList(ImportEsExampleConfig config) throws Exception {
		return ScanUtil.scan(config.getSrcPath(), config.getPattern(), config.isDelMatchFail());
	}

	@Override
	protected Executor createExecutor(String sourceUri, ImportEsExampleConfig config) throws Exception {
		ImportEsExampleHelper helper = new ImportEsExampleHelper(getName(), config.getEsIndex(), config.getEsType()); 
		ImportEsExampleExecutor executor = new ImportEsExampleExecutor(getName(), sourceUri, sourceUri,
				config.getBatch(), helper, config.getEsIndex(), config.getEsType(),  config.getEsJsonFile());
		return executor;
	}
}
