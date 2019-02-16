package com.fom.examples;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Task;
import com.fom.context.task.ZipParseTask;
import com.fom.util.FileUtil;
import com.fom.util.PatternUtil;

/**
 * 
 * @author shanhm
 *
 */
public class ImportOracleExample2 extends Context{

	private static final long serialVersionUID = -8057924720840191559L;

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;
	
	private String pattern;
	
	public ImportOracleExample2(String name){
		super(name);
		srcPath = ContextUtil.getContextPath(getString("srcPath", ""));
		batch = getInt("batch", 5000);
		isDelMatchFail = getBoolean("isDelMatchFail", false);
		pattern = getString("pattern", "");
	}

	@Override
	protected List<String> getTaskIdList() throws Exception {
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
	protected Task createTask(String sourceUri) throws Exception {
		String subPettern = getString("zipEntryPattern", "");
		ImportOracleExample2Helper helper = new ImportOracleExample2Helper(getName(), subPettern);
		return new ZipParseTask(sourceUri, batch, helper);
	}
}
