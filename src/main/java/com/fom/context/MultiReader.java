package com.fom.context;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import com.fiberhome.odin.hadoop.hdfs.io.SDFileReader;
import com.fom.util.IoUtils;

/**
 * 引入SDFileReader是为了解析orc压缩格式的文件
 * 但是存在bug场景，在windows下没有hadoop环境变量时，即便关闭了SDFileReader，依然无法删除文件
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class MultiReader implements Closeable {
	
	private boolean useSD;
	
	private Configuration fsConf;
	
	private SDFileReader sdReader;
	
	private BufferedReader buffReader;

	
	public MultiReader(File file, boolean useSD) throws Exception{
		this.useSD = useSD;
		if(useSD){
			fsConf = new Configuration();
			fsConf.set("fs.defaultFS", "file:///");
			sdReader = new SDFileReader(new Path(file.getPath()), fsConf);
		}else{
			buffReader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
		}
	}
	
	public String readLine() throws Exception{
		if(useSD){
			return sdReader.readStringLine();
		}else{
			return buffReader.readLine();
		}
	}
	
	@Override
	public void close(){
		IoUtils.close(sdReader);
		IoUtils.close(buffReader);
	}

}
