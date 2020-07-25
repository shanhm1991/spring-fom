package example.fom.batchschedul.mybatis;

import java.util.Arrays;
import java.util.List;

import org.eto.fom.context.SpringContext;
import org.eto.fom.task.parse.ParseTextTask;
import org.eto.fom.util.file.reader.IReader;
import org.eto.fom.util.file.reader.IRow;
import org.eto.fom.util.file.reader.TextReader;

import example.fom.batchschedul.dao.ExamplesDao;
import example.fom.xml.mybatis.bean.ExampleBean;

/**
 * 
 * @author shanhm
 *
 */
public class InputMysqlTask1 extends ParseTextTask<ExampleBean> {
	
	public InputMysqlTask1(String sourceUri, int batch){
		super(sourceUri, batch); 
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
		ExamplesDao demoDao = SpringContext.getBean("mysqlExampleDao", ExamplesDao.class);
		demoDao.batchInsert(lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
	}

}
