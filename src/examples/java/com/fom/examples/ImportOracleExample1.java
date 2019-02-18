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
public class ImportOracleExample1 extends Context {

	private static final long serialVersionUID = 9068696410626792009L;

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;
	
	private String pattern;

	public ImportOracleExample1(){
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
	protected Task createTask(String taskId) throws Exception { 
		String subPettern = getString("zipEntryPattern", "");
		ImportOracleExample1Helper helper = new ImportOracleExample1Helper(getName(), subPettern);
		return new ZipParseTask(taskId, batch, helper);
	}
}
