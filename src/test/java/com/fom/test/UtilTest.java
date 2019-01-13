package com.fom.test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.orc.OrcFile;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.junit.Test;
import org.quartz.CronExpression;

import com.fom.util.HttpUtil;
import com.fom.util.IoUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class UtilTest {

	@Test
	public void cron() throws ParseException{
		CronExpression cron = new CronExpression("0 0 0/1 * * ?");
		Date nextDate = cron.getNextValidTimeAfter(new Date());
		System.out.println(new SimpleDateFormat("YYYYMMdd HH:mm:ss SSS").format(nextDate));
	}

	@Test
	public void pattern(){
		Pattern pattern = Pattern.compile(".\\.txt$");
		System.out.println(pattern.matcher("新建文本文档.txt").find()); 
	}

	@Test
	public void xml() throws IOException, DocumentException{ 
		String s = "<oprator name=\"test\">"
				+"<importer/>"
				+"<scanner/>"
				+"<src.type>zip</src.type>"
				+"<src.zip.subPattern/>"
				+"<src.match.fail.del>false</src.match.fail.del>"
				+"<importer.batch>5000</importer.batch>"
				+"<importer.max>10</importer.max>"
				+"<importer.aliveTime.seconds>30</importer.aliveTime.seconds>"
				+"<importer.overTime.seconds>86400</importer.overTime.seconds>"
				+"<importer.overTime.cancle>false</importer.overTime.cancle>"
				+"<external>"
				+"</external>"
				+"</oprator>";

		SAXReader reader = new SAXReader();
		StringReader in=new StringReader(s);  
		Document doc=reader.read(in); 

		OutputFormat formater=OutputFormat.createPrettyPrint();  
		formater.setEncoding("UTF-8");  
		StringWriter out=new StringWriter();  
		XMLWriter writer=new XMLWriter(out,formater);
		writer.write(doc);  
		writer.close();  
		System.out.println(out.toString());
	}

	@Test
	public void readOrc() { 
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "file:///");

		Reader reader;
		try {
			reader = OrcFile.createReader(new Path("E:/node.txt"), OrcFile.readerOptions(conf));
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
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void http() { 
		HttpGet httpGet = new HttpGet("https://www.cnblogs.com/shanhm1991/p/9906917.html");
		httpGet.setHeader("User-Agent", "Mozilla/5.0");
		httpGet.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
		httpGet.setHeader("Accept-Charset", "ISO-8859-1,utf-8,gbk,gb2312;q=0.7,*;q=0.7");
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(3000)
				.setConnectTimeout(3000)
				.setSocketTimeout(3000)
				.build();
		httpGet.setConfig(requestConfig); 

		try{
			CloseableHttpResponse response = HttpUtil.request(httpGet);
			HttpEntity entity = response.getEntity();
			System.out.println(EntityUtils.toString(entity, "utf-8")) ;
			EntityUtils.consume(entity);
			IoUtil.close(response); 
		}catch(Exception e){
			e.printStackTrace();
		}

	}
}
