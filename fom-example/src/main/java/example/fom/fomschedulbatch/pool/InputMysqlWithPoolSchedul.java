package example.fom.fomschedulbatch.pool;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomConfig;
import org.eto.fom.context.annotation.FomSchedulBatch;
import org.eto.fom.context.annotation.SchedulBatchFactory;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.FileUtil;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedulBatch(name = "InputMysqlWithPoolDemo", cron = "0/35 * * * * ?", remark = "将指定目录下text文本解析导入Mysql")
public class InputMysqlWithPoolSchedul implements SchedulBatchFactory {

	@FomConfig(value = "/source")
	private String srcPath;

	@FomConfig("test.xlsx")
	private String pattern; 
	
	@FomConfig("5000")
	private int batch;
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<InputMysqlWithPoolTask> creatTasks() throws Exception {
		List<String> list = FileUtil.list(SpringContext.getPath(srcPath), new FileFilter(){
			@Override
			public boolean accept(File file) {
				return PatternUtil.match(pattern, file.getName());
			}
		}); 
		
		Set<InputMysqlWithPoolTask> set = new HashSet<>();
		for(String uri : list){
			set.add(new InputMysqlWithPoolTask(uri, batch));
		}
		return set;
	}
}
