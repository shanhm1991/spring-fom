package com.fom.context;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.FileFormatException;
import org.apache.orc.OrcFile;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;

import com.fom.util.IoUtils;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class MultiReader implements Closeable {


	private BufferedReader buffReader;

	private RecordReader recordReader;

	private VectorizedRowBatch batch;
	
	private StringBuilder builder;

	private boolean isOrc;


	public MultiReader(File file) throws Exception {
		try {
			Reader reader = 
					OrcFile.createReader(new Path("E:/node.txt"), OrcFile.readerOptions(new Configuration()));
			recordReader = reader.rows();
			batch = reader.getSchema().createRowBatch(1);
			builder = new StringBuilder();
			isOrc = true;
		} catch (FileFormatException e) {
			buffReader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
		}
	}

	public String readLine() throws Exception{
		if(!isOrc){
			return buffReader.readLine();
		}
		
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
	public void close(){
		IoUtils.close(recordReader);
		IoUtils.close(buffReader);
	}

}
