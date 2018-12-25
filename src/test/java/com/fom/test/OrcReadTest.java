package com.fom.test;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.OrcFile;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;

public class OrcReadTest {


	public static void main(String[] args) throws IllegalArgumentException, IOException {
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "file:///");
		
		Reader reader = OrcFile.createReader(new Path("E:/node.txt"), OrcFile.readerOptions(conf));
		RecordReader rows = reader.rows();
		VectorizedRowBatch batch = reader.getSchema().createRowBatch(1);
		while (rows.nextBatch(batch)) {
			int colums = batch.numCols;
			for (int r = 0;r < batch.size;r++) { //row
				for(int c = 0;c < colums;c++){
					System.out.print(batch.cols[c]);
				}
				System.out.println();
			}
		}
		rows.close();
	}

}
