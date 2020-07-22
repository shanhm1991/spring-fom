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
public class InputEsExample extends Context {

	private static final long serialVersionUID = 4614661907354806328L;

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;

	private String pattern;

	private String esIndex;

	private String esType;

	private File esJson;
	
	public InputEsExample() {
		srcPath = ServletUtil.getContextPath(config.getString("srcPath", ""));
		pattern = config.getString("pattern", "");
		batch = config.getInt("batch", 5000);
		isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		esIndex = config.getString("esIndex", "");
		esType = config.getString("esType", "");
		esJson = new File(ServletUtil.getContextPath((config.getString("esJson", "")))); 
	}

	public InputEsExample(String name){
		super(name);
		srcPath = ServletUtil.getContextPath(config.getString("srcPath", ""));
		pattern = config.getString("pattern", "");
		batch = config.getInt("batch", 5000);
		isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		esIndex = config.getString("esIndex", "");
		esType = config.getString("esType", "");
		esJson = new File(ServletUtil.getContextPath((config.getString("esJson", "")))); 
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Collection<InputEsExampleTask> scheduleBatch() throws Exception { 
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
		
		Set<InputEsExampleTask> set = new HashSet<>();
		for(String path : list){
			set.add(new InputEsExampleTask(path, batch, esIndex, esType,  esJson));
		}
		return set;
	}
}
