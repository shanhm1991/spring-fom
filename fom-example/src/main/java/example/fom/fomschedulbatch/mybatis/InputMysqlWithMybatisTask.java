package example.fom.fomschedulbatch.mybatis;

import java.util.Arrays;
import java.util.List;

import org.eto.fom.task.parse.ParseTextTask;
import org.eto.fom.util.file.reader.IReader;
import org.eto.fom.util.file.reader.IRow;
import org.eto.fom.util.file.reader.TextReader;

import example.fom.fomcontextxml.mybatis.bean.ExampleBean;
import example.fom.fomschedulbatch.mybatis.service.impl.InputMysqServiceImpl;

/**
 * 
 * @author shanhm
 *
 */
public class InputMysqlWithMybatisTask extends ParseTextTask<ExampleBean> {
	
	private InputMysqServiceImpl service;
	
	public InputMysqlWithMybatisTask(String sourceUri, int batch, InputMysqServiceImpl service){
		super(sourceUri, batch); 
		this.service = service;
	}

	@Override
	public IReader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	public List<ExampleBean> parseRowData(IRow rowData, long batchTime) throws Exception {
		ExampleBean bean = new ExampleBean(rowData.getColumnList());
		bean.setSource("local");
		bean.setFileType("txt");
		bean.setImportWay("mybatis");
		return Arrays.asList(bean);
	}
	
	@Override
	public void batchProcess(List<ExampleBean> lineDatas, long batchTime) throws Exception {
		service.input(lineDatas);
	}

}
