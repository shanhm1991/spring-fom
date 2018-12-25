package com.fom.test;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class SDReadTest {

	public static void main(String[] args) throws IOException {
		Configuration fsConf = new Configuration();
		fsConf.set("fs.defaultFS", "file:///");
		FileSystem fs = FileSystem.get(fsConf);

		Path path = null;
//		File file = new File("E:/node.txt");
//		String line = "";
//		SDFileReader reader = null;
//		try{
//			path = new Path(file.getPath());
//			reader = new SDFileReader(path, fsConf);
//			while ((line = reader.readStringLine()) != null) {
//				System.out.println(line);
//			}
//		}finally{
//			IoUtils.close(reader);
//		}
		System.out.println(fs.delete(path, true)); //bug
	}
}
