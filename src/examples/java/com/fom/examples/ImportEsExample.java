package com.fom.examples;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Executor;
import com.fom.util.FileUtil;
import com.fom.util.PatternUtil;

/**
 * 
 * @author shanhm
 *
 */
public class ImportEsExample extends Context {

	private static final long serialVersionUID = 4614661907354806328L;

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;

	private String pattern;

	private String esIndex;

	private String esType;

	private File esJson;
	
	public ImportEsExample() {
		srcPath = ContextUtil.getContextPath(getString("srcPath", ""));
		pattern = getString("pattern", "");
		batch = getInt("batch", 5000);
		isDelMatchFail = getBoolean("isDelMatchFail", false);
		esIndex = getString("esIndex", "");
		esType = getString("esType", "");
		esJson = new File(ContextUtil.getContextPath((getString("esJson", "")))); 
	}

	public ImportEsExample(String name){
		super(name);
		srcPath = ContextUtil.getContextPath(getString("srcPath", ""));
		pattern = getString("pattern", "");
		batch = getInt("batch", 5000);
		isDelMatchFail = getBoolean("isDelMatchFail", false);
		esIndex = getString("esIndex", "");
		esType = getString("esType", "");
		esJson = new File(ContextUtil.getContextPath((getString("esJson", "")))); 
	}

	@Override
	protected List<String> getUriList() throws Exception {
		return FileUtil.list(srcPath, new FileFilter(){
			@Override
			public boolean accept(File file) {
				if(!PatternUtil.match(pattern, file.getName())){
					if(isDelMatchFail && !file.delete()){
						log.warn("删除文件[不匹配]失败:" + name);
					}
					return false;
				}
				return true;
			}
		}); 
	}

	@Override
	protected Executor createExecutor(String sourceUri) throws Exception {
		ImportEsExampleHelper helper = new ImportEsExampleHelper(getName(), esIndex, esType); 
		ImportEsExampleExecutor executor = 
				new ImportEsExampleExecutor(sourceUri, batch, helper, esIndex, esType,  esJson);
		return executor;
	}
}
