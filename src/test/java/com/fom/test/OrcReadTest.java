package com.fom.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * 
 * @author shanhm
 *
 */
public class OrcReadTest {

	public static void main(String[] args) {
//		Configuration conf = new Configuration();
//		conf.set("fs.defaultFS", "file:///");
//
//		Reader reader;
//		try {
//			reader = OrcFile.createReader(new Path("E:/node.txt"), OrcFile.readerOptions(conf));
//			RecordReader rows = reader.rows();
//			VectorizedRowBatch batch = reader.getSchema().createRowBatch(1);
//			while (rows.nextBatch(batch)) {
//				int colums = batch.numCols;
//				for (int r = 0;r < batch.size;r++) { //row
//					for(int c = 0;c < colums;c++){
//						System.out.print(batch.cols[c]);
//					}
//					System.out.println();
//				}
//			}
//			rows.close();
//		} catch (Exception e) { 
//			System.out.println("文件非法");
//		} 
		
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, -10);
		
		System.out.println(new SimpleDateFormat("yyyyMMdd").format(c.getTime()));
		
		
	}
}
