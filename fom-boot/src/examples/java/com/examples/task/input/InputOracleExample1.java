package com.examples.task.input;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eto.fom.boot.ServletUtil;
import org.eto.fom.context.core.Context;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.FileUtil;

/**
 * 	
 * @author shanhm
 *
 */
public class InputOracleExample1 extends Context {

	private static final long serialVersionUID = 9068696410626792009L;

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;
	
	private String pattern;

	public InputOracleExample1(){
		srcPath = ServletUtil.getContextPath(config.getString("srcPath", ""));
		batch = config.getInt("batch", 5000);
		isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		pattern = config.getString("pattern", "");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Collection<InputOracleExample1Task> scheduleBatch() throws Exception { 
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
		
		Set<InputOracleExample1Task> set = new HashSet<>();
		String subPettern = config.getString("zipEntryPattern", "");
		for(String uri : list){
			set.add(new InputOracleExample1Task(uri, batch, subPettern));
		}
		return set;
	}
}
