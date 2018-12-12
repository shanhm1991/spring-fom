package com.fom.web;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fiberhome.odin.hadoop.hdfs.io.SDFileReader;
import com.fom.util.IoUtils;
import com.fom.util.log.LoggerFactory;

@Controller
public class TaskController {

	private static Logger log = LoggerFactory.getLogger("web"); 

	@RequestMapping("/hellofm")
	public String hello() throws IOException{
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
				log.info(line);
			}
		}finally{
			IoUtils.close(reader);
		}
		return "monitor";
	}
}
