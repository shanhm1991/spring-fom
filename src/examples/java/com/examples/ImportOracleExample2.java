package com.examples;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fom.context.Context;
import com.fom.context.ContextUtil;
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
		srcPath = ContextUtil.getContextPath(config.getString("srcPath", ""));
		batch = config.getInt("batch", 5000);
		isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		pattern = config.getString("pattern", "");
	}

	@Override
	protected Set<ImportOracleExample2Task> scheduleBatchTasks() throws Exception { 
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
		
		Set<ImportOracleExample2Task> set = new HashSet<>();
		String subPettern = config.getString("zipEntryPattern", "");
		for(String uri : list){
			set.add(new ImportOracleExample2Task(uri, batch, subPettern));
		}
		return set;
	}
}
