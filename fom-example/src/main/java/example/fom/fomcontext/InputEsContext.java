package example.fom.fomcontext;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.FileUtil;
import org.springframework.beans.factory.annotation.Value;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(cron = "0 0 18 * * ?", remark = "将指定目录下text文本解析导入ES")
public class InputEsContext extends Context<Boolean> {

	@Value("${input.srcPath:/source}")
	private String srcPath;

	@Value("${input.pattern:demo.bcp}")
	private String pattern;

	@Value("${input.esIndex:demo}")
	private String esIndex;

	@Value("${input.esType:demo}")
	private String esType;
	
	@Value("${input.batch:5000}")
	private int batch;

	@Value("${input.esJson:config/fomcontext/index.json}")
	private String esJson;
	
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
