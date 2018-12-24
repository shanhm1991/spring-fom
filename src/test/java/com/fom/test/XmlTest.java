package com.fom.test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class XmlTest {
	public static void main(String[] args) throws DocumentException, IOException {

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
}
