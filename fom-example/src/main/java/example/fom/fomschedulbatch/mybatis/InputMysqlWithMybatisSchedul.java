package example.fom.fomschedulbatch.mybatis;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eto.fom.context.SpringContext;
import org.eto.fom.context.annotation.FomSchedulBatch;
import org.eto.fom.context.annotation.SchedulBatchFactory;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;

import example.fom.fomschedulbatch.mybatis.service.impl.InputMysqServiceImpl;
import org.springframework.beans.factory.annotation.Value;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedulBatch(cron = "0/15 * * * * ?", remark = "将指定目录下text文本解析导入Mysql")
public class InputMysqlWithMybatisSchedul implements SchedulBatchFactory<Boolean> {

	@Value("${input.srcPath:/source}")
	private String srcPath;

	@Value("${input.pattern:demo.txt}")
	private String pattern;
	
	@Value("${input.batch:5000}")
	private int batch;
	
	@Autowired
	private InputMysqServiceImpl service;
	
	@Override
	public Collection<InputMysqlWithMybatisTask> creatTasks() throws Exception {
		List<String> list = FileUtil.list(SpringContext.getPath(srcPath), new FileFilter(){
			@Override
			public boolean accept(File file) {
				return PatternUtil.match(pattern, file.getName());
			}
		}); 
		
		Set<InputMysqlWithMybatisTask> set = new HashSet<>();
		for(String uri : list){
			set.add(new InputMysqlWithMybatisTask(uri, batch, service));
		}
		return set;
	}
}
