package example.fom.fomcontextxml.pool;

import java.io.File;
import java.io.FileFilter;
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
public class InputOracleWithPoolContext extends Context{

	@SuppressWarnings("unchecked")
	@Override
	protected Collection<InputOracleWithPoolTask> scheduleBatch() throws Exception { 
		String srcPath = SpringContext.getPath(config.getString("srcPath", ""));
		int batch = config.getInt("batch", 5000);
		boolean isDelMatchFail = config.getBoolean("isDelMatchFail", false);
		String pattern = config.getString("pattern", "");
		
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
		
		Set<InputOracleWithPoolTask> set = new HashSet<>();
		String subPettern = config.getString("zipEntryPattern", "");
		for(String uri : list){
			set.add(new InputOracleWithPoolTask(uri, batch, subPettern));
		}
		return set;
	}
}
