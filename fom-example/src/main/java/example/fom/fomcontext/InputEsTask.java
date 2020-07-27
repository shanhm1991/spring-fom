package example.fom.fomcontext;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eto.fom.task.parse.ParseTextTask;
import org.eto.fom.util.file.reader.IReader;
import org.eto.fom.util.file.reader.IRow;
import org.eto.fom.util.file.reader.TextReader;
import org.eto.fom.util.pool.handler.EsHandler;

/**
 * 
 * @author shanhm
 *
 */
public class InputEsTask extends ParseTextTask<Map<String, Object>> {
	
	private static final String POOL = "example_es";
	
	private String esIndex;
	
	private String esType;
	
	private File esJson;

	public InputEsTask(String sourceUri, int batch, String esIndex, String esType, File esJson) {
		super(sourceUri, batch); 
		this.esIndex = esIndex;
		this.esType = esType;
		this.esJson = esJson;
	}

	@Override
	protected boolean beforeExec() throws Exception {
		if(super.beforeExec() && EsHandler.handler.synCreateIndex(POOL, esIndex, esType, esJson)){
			log.info("创建ES索引[index=" + "demo" + ", type=" + "demo" + "]");
			return true;
		}
		return false;
	}

	@Override
	protected IReader getReader(String sourceUri) throws Exception {
		return new TextReader(sourceUri, "#");
	}

	@Override
	protected List<Map<String, Object>> parseRowData(IRow rowData, long batchTime) throws Exception {
		List<String> columns = rowData.getColumnList();
		Map<String,Object> map = new HashMap<>();
		map.put("ID", columns.get(0));
		map.put("NAME", columns.get(1)); 
		map.put("SOURCE", "local");
		map.put("FILETYPE", "txt");
		map.put("IMPORTWAY", "pool");
		return Arrays.asList(map);
	}

	@Override
	protected void batchProcess(List<Map<String, Object>> batchData, long batchTime) throws Exception {
		Map<String,Map<String,Object>> map = new HashMap<>();
		for(Map<String, Object> m : batchData){
			map.put(String.valueOf(m.get("ID")), m);
		}
		EsHandler.handler.bulkInsert(POOL, esIndex, esType, map); 
		log.info("处理数据入库:" + map.size());
	}

}
