package example.fom.fomcontextxml.mybatis;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;

import example.fom.fomcontextxml.mybatis.service.impl.InputOracleServiceImpl;

/**
 * 
 * @author shanhm
 *
 */
public class InputOracleWithMybatisContext extends Context<Boolean> {

	@Autowired
	private InputOracleServiceImpl service; //TODO 这里类型写InputOracleService会提示两个实例错误，不知道为什么
	
	@Override
	protected Collection<InputOracleWithMybatisTask> scheduleBatch() throws Exception { 
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
		
		String subPettern = config.getString("zipEntryPattern", "");

		List<InputOracleWithMybatisTask> tasks = new ArrayList<>();
		for(String uri : list){
			tasks.add(new InputOracleWithMybatisTask(uri, batch, subPettern, service));
		}
		return tasks;
	}
}
