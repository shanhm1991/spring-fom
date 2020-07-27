package example.fom.fomschedulbatch.mybatis;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eto.fom.context.annotation.FomConfig;
import org.eto.fom.context.annotation.FomSchedulBatch;
import org.eto.fom.context.annotation.SchedulBatchFactory;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;

import example.fom.fomschedulbatch.mybatis.service.impl.InputMysqServiceImpl;

/**
 * 
 * @author shanhm
 *
 */
@FomSchedulBatch
public class InputMysqlWithMybatisSchedul implements SchedulBatchFactory {

	@FomConfig(key = "srcPath", value = "/source")
	private String srcPath;

	@FomConfig("demo.txt")
	private String pattern;
	
	@FomConfig("5000")
	private int batch;
	
	@Autowired
	private InputMysqServiceImpl service;
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<InputMysqlWithMybatisTask> creatTasks() throws Exception {
		List<String> list = FileUtil.list(srcPath, new FileFilter(){
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
