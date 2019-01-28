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
public class ImpotEsExample extends Context<ImpotEsExampleConfig> {
	
	@Override
	protected List<String> getUriList(ImpotEsExampleConfig config) throws Exception {
		return ScanUtil.scan(config.getSrcPath(), config.getPattern(), config.isDelMatchFail());
	}

	@Override
	protected Executor createExecutor(String sourceUri, ImpotEsExampleConfig config) throws Exception {
		ImpotEsExampleHelper helper = new ImpotEsExampleHelper(getName(), config.getEsIndex(), config.getEsType()); 
		ImpotEsExampleExecutor executor = new ImpotEsExampleExecutor(getName(), sourceUri, sourceUri,
				config.getBatch(), helper, config.getEsIndex(), config.getEsType(),  config.getEsJsonFile());
		return executor;
	}
}
