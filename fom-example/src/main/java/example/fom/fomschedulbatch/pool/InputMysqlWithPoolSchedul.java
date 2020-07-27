package example.fom.fomschedulbatch.pool;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.FileUtil;

/**
 * 
 * @author shanhm
 *
 */
public class InputMysqlWithPoolSchedul extends Context {

	
	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;
	
	private String pattern; 
	
	public InputMysqlWithPoolSchedul() throws IOException{
		srcPath = SpringContext.getPath(config.getString("srcPath", ""));
		batch = config.getInt("batch", 5000);
		isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		pattern = config.getString("pattern", "");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Collection<InputMysqlWithPoolTask> scheduleBatch() throws Exception { 
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
		
		Set<InputMysqlWithPoolTask> set = new HashSet<>();
		for(String uri : list){
			set.add(new InputMysqlWithPoolTask(uri, batch));
		}
		return set;
	}
}
