package com.fom.test;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import com.fiberhome.odin.hadoop.hdfs.io.SDFileReader;
import com.fom.util.IoUtils;

public class ReadFileTest {

	public static void main(String[] args) throws IOException {
		Configuration fsConf = new Configuration();
		fsConf.set("fs.defaultFS", "file:///");

		File file = new File("E:/node.txt");
		int StartLine = 0;
		int lineIndex = 0;
		SDFileReader reader = null;
		String line = "";
		try{
			Path path = new Path(file.getPath());
			reader = new SDFileReader(path, fsConf);

			while ((line = reader.readStringLine()) != null) {
				lineIndex++;
				if(lineIndex <= StartLine){
					continue;
				}
				System.out.println(line);
			}
		}finally{
			IoUtils.close(reader);
		}
		System.out.println(file.delete()); 
	}
}
