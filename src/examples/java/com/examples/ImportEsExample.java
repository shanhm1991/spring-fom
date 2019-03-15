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
		srcPath = ContextUtil.getContextPath(config.getString("srcPath", ""));
		pattern = config.getString("pattern", "");
		batch = config.getInt("batch", 5000);
		isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		esIndex = config.getString("esIndex", "");
		esType = config.getString("esType", "");
		esJson = new File(ContextUtil.getContextPath((config.getString("esJson", "")))); 
	}

	public ImportEsExample(String name){
		super(name);
		srcPath = ContextUtil.getContextPath(config.getString("srcPath", ""));
		pattern = config.getString("pattern", "");
		batch = config.getInt("batch", 5000);
		isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		esIndex = config.getString("esIndex", "");
		esType = config.getString("esType", "");
		esJson = new File(ContextUtil.getContextPath((config.getString("esJson", "")))); 
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Set<ImportEsExampleTask> scheduleBatchTasks() throws Exception { 
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
		
		Set<ImportEsExampleTask> set = new HashSet<>();
		for(String path : list){
			set.add(new ImportEsExampleTask(path, batch, esIndex, esType,  esJson));
		}
		return set;
	}
}
