package example.fom.context;

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
public class InputEsContext extends Context {

	private String srcPath;

	private int batch;

	private boolean isDelMatchFail;

	private String pattern;

	private String esIndex;

	private String esType;

	private File esJson;
	
	public InputEsContext() throws IOException {
		srcPath = SpringContext.getPath(config.getString("srcPath", ""));
		pattern = config.getString("pattern", "");
		batch = config.getInt("batch", 5000);
		isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		esIndex = config.getString("esIndex", "");
		esType = config.getString("esType", "");
		esJson = new File(SpringContext.getPath((config.getString("esJson", "")))); 
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Collection<InputEsTask> scheduleBatch() throws Exception { 
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
		
		Set<InputEsTask> set = new HashSet<>();
		for(String path : list){
			set.add(new InputEsTask(path, batch, esIndex, esType,  esJson));
		}
		return set;
	}
}
