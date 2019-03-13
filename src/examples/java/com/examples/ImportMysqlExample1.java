package com.examples;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.examples.bean.ExampleBean;
import com.fom.context.Context;
import com.fom.context.ContextUtil;
import com.fom.context.Task;
import com.fom.task.TxtParseTask;
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
		srcPath = ContextUtil.getContextPath(config.getString("srcPath", ""));
		batch = config.getInt("batch", 5000);
		isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		pattern = config.getString("pattern", "");
	}

	@Override
	protected Set<Task> scheduleBatchTasks() throws Exception { 
		List<String> list = FileUtil.list(srcPath, new FileFilter(){
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
		
		Set<Task> set = new HashSet<>();
		ImportMysqlExample1Helper helper = new ImportMysqlExample1Helper(getName());
		for(String uri : list){
			set.add(new TxtParseTask<ExampleBean>(uri, batch, helper));
		}
		return set;
	}
}
