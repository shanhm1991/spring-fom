package com.fom.examples;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Task;
import com.fom.examples.bean.ExampleBean;
import com.fom.task.ParseTask;
import com.fom.util.FileUtil;
import com.fom.util.PatternUtil;

/**
 * 
 * @author shanhm 
 *
 */
public class ImportMysqlExample1 extends Context {
	
	private static final long serialVersionUID = -5846681676399683500L;

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;
	
	private String pattern;
	
	public ImportMysqlExample1(){
		srcPath = ContextUtil.getContextPath(getString("srcPath", ""));
		batch = getInt("batch", 5000);
		isDelMatchFail = getBoolean("isDelMatchFail", false);
		pattern = getString("pattern", "");
	}

	@Override
	protected Set<String> getTaskIdSet() throws Exception { 
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
	protected Task cronBatchSubmitTask(String taskId) throws Exception { 
		ImportMysqlExample1Helper helper = new ImportMysqlExample1Helper(getName());
		return new ParseTask<ExampleBean>(taskId, batch, helper);
	}
}
