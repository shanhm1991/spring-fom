package example.fom.fomcontext;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomConfig;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.FileUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(cron = "0 0 18 * * ?", remark = "将指定目录下text文本解析导入ES")
public class InputEsContext extends Context {

	@FomConfig("/source")
	private String srcPath;

	@FomConfig("demo.bcp")
	private String pattern;

	@FomConfig("demo")
	private String esIndex;

	@FomConfig("demo")
	private String esType;
	
	@FomConfig("5000")
	private int batch;

	@FomConfig("config/fomcontext/index.json")
	private String esJson;
	
	@SuppressWarnings("unchecked")
	@Override
	protected Collection<InputEsTask> scheduleBatch() throws Exception { 
		List<String> list = FileUtil.list(SpringContext.getPath(srcPath), new FileFilter(){
			@Override
			public boolean accept(File file) {
				return PatternUtil.match(pattern, file.getName());
			}
		}); 
		
		Set<InputEsTask> set = new HashSet<>();
		for(String path : list){
			set.add(new InputEsTask(path, batch, esIndex, esType,  new File(SpringContext.getPath(esJson))));
		}
		return set;
	}
}
