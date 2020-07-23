package org.eto.fom.example.xml;

import java.util.Arrays;
import java.util.List;

import org.eto.fom.context.SpringContext;
import org.eto.fom.example.batchschedul.ExampleBean;
import org.eto.fom.example.batchschedul.dao.ExamplesDao;
import org.eto.fom.task.parse.ParseTextZipTask;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.reader.IReader;
import org.eto.fom.util.file.reader.IRow;
import org.eto.fom.util.file.reader.TextReader;

/**
 * 
 * @author shanhm
 *
 */
public class InputOracleTask1 extends ParseTextZipTask<ExampleBean> {
	
	private final String pattern;
	
	public InputOracleTask1(String sourceUri, int batch, String pattern) {
		super(sourceUri, batch); 
		this.pattern = pattern;
	}

	@Override
	public IReader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	public List<ExampleBean> parseRowData(IRow rowData, long batchTime) throws Exception {
		ExampleBean bean = new ExampleBean(rowData.getColumnList());
		bean.setSource("local");
		bean.setFileType("zip(txt)");
		bean.setImportWay("mybatis");
		return Arrays.asList(bean);
	}
	
	@Override
	public void batchProcess(List<ExampleBean> lineDatas, long batchTime) throws Exception {
		ExamplesDao demoDao = SpringContext.getBean("oracleExampleDao", ExamplesDao.class);
		demoDao.batchInsert(lineDatas);
		log.info("处理数据入库:" + lineDatas.size());
		
	}

	@Override
	public boolean matchEntryName(String entryName) {
		return PatternUtil.match(pattern, entryName);
	}
	
}
