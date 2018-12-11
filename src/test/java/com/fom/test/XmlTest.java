package com.fom.test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class XmlTest {
	public static void main(String[] args) throws DocumentException, IOException {

		String s = "<oprator name=\"merchineLearn_user\" config=\"com.fom.modules.importer.merchineLearn.MerchineLearnConfig\">"
				+"<importer/>"
				+"<scanner/>"
				+"<src.path>/home/wxy/mlinput</src.path>"
				+"<src.scan.cron>0 0/3 * * * ?</src.scan.cron>"
				+"<src.pattern>WXY\\d{5}_GROUP_DATA_\\d{8}_\\d+_\\d+.zip$</src.pattern>"
				+"<src.isHdfs>true</src.isHdfs>"
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
