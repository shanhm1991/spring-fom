package org.spring.fom.support.task.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.OrcFile;
import org.apache.orc.RecordReader;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class OrcReader implements IReader {
	
	private RecordReader recordReader;

	private VectorizedRowBatch batch;
	
	private int rowIndex;
	
	private StringBuilder builder = new StringBuilder();
	
	/**
	 * 
	 * @param sourceUri sourceUri
	 * @param configuration configuration
	 * @throws Exception Exception
	 */
	public OrcReader(String sourceUri, Configuration configuration) throws Exception{   
		org.apache.orc.Reader reader = 
				OrcFile.createReader(new Path(sourceUri), OrcFile.readerOptions(configuration));
		recordReader = reader.rows();
		batch = reader.getSchema().createRowBatch(1);
	}
	
	/**
	 * 
	 * @param path path
	 * @param configuration configuration
	 * @throws Exception Exception
	 */
	public OrcReader(Path path, Configuration configuration) throws Exception{  
		org.apache.orc.Reader reader = 
				OrcFile.createReader(path, OrcFile.readerOptions(configuration));
		recordReader = reader.rows();
		batch = reader.getSchema().createRowBatch(1);
	}

	@Override
	public TextRow readRow() throws Exception { 
		if(recordReader.nextBatch(batch)){
			rowIndex++;
			List<String> list = new ArrayList<>();
			for(int i = 0;i < batch.numCols;i++){
				batch.cols[i].stringifyValue(builder, 0);
				list.add(builder.toString());
				builder.setLength(0); 
			}
			TextRow row = new TextRow(rowIndex - 1, list);
			row.setEmpty(false);
			return row;
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		if(recordReader != null){
			try{
				recordReader.close();
			}catch(Exception e){
				
			}
		}
	}
}
