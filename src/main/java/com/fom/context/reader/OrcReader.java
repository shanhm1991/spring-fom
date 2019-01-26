package com.fom.context.reader;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.OrcFile;
import org.apache.orc.RecordReader;

import com.fom.util.IoUtil;

/**
 * 
 * @author shanhm
 *
 */
public class OrcReader implements Reader {
	
	private RecordReader recordReader;

	private VectorizedRowBatch batch;
	
	private StringBuilder builder;
	
	/**
	 * 
	 * @param sourceUri
	 * @param configuration
	 * @throws Exception
	 */
	public OrcReader(String sourceUri, Configuration configuration) throws Exception{   
		org.apache.orc.Reader reader = 
				OrcFile.createReader(new Path(sourceUri), OrcFile.readerOptions(configuration));
		recordReader = reader.rows();
		batch = reader.getSchema().createRowBatch(1);
		builder = new StringBuilder();
	}
	
	/**
	 * 
	 * @param path
	 * @param configuration
	 * @throws Exception
	 */
	public OrcReader(Path path, Configuration configuration) throws Exception{  
		org.apache.orc.Reader reader = 
				OrcFile.createReader(path, OrcFile.readerOptions(configuration));
		recordReader = reader.rows();
		batch = reader.getSchema().createRowBatch(1);
		builder = new StringBuilder();
	}

	@Override
	public String readLine() throws Exception { 
		if(recordReader.nextBatch(batch)){
			builder.setLength(0); 
			for(int i = 0;i < batch.numCols;i++){
				batch.cols[i].stringifyValue(builder, 0);
				builder.append("\t");
			}
			return builder.toString();
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		IoUtil.close(recordReader);
	}
}
