package example.fom.fomcontextxml.mybatis;

import java.util.Arrays;
import java.util.List;

import org.eto.fom.task.parse.ParseTextZipTask;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.reader.IReader;
import org.eto.fom.util.file.reader.IRow;
import org.eto.fom.util.file.reader.TextReader;

import example.fom.fomcontextxml.mybatis.bean.ExampleBean;
import example.fom.fomcontextxml.mybatis.service.InputOracleService;

/**
 * 
 * @author shanhm
 *
 */
public class InputOracleWithMybatisTask extends ParseTextZipTask<ExampleBean> {
	
	private final String pattern;
	
	private final InputOracleService service;
	
	public InputOracleWithMybatisTask(String sourceUri, int batch, String pattern, InputOracleService service) {
		super(sourceUri, batch); 
		this.pattern = pattern;
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
		bean.setFileType("zip(txt)");
		bean.setImportWay("mybatis");
		return Arrays.asList(bean);
	}
	
	@Override
	public void batchProcess(List<ExampleBean> lineDatas, long batchTime) throws Exception {
		service.input(lineDatas);
	}

	@Override
	public boolean matchEntryName(String entryName) {
		return PatternUtil.match(pattern, entryName);
	}
	
}
