package example.fom.fomschedulbatch.pool;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomSchedulBatch;
import org.eto.fom.context.annotation.SchedulBatchFactory;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.FileUtil;
import org.springframework.beans.factory.annotation.Value;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedulBatch(name = "InputMysqlWithPoolDemo", cron = "0/35 * * * * ?", remark = "将指定目录下text文本解析导入Mysql")
public class InputMysqlWithPoolSchedul implements SchedulBatchFactory<Boolean> {

	@Value("${input.srcPath:/source}")
	private String srcPath;

	@Value("${input.pattern:test.xlsx}")
	private String pattern;

	@Value("${input.batch:5000}")
	private int batch;
	
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
