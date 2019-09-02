package org.eto.fom.util.file.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.eto.fom.util.IoUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * 
 * @author shanhm
 *
 */
public class ExcelEventReader implements Reader {

	private String sheetName;

	private int sheetIndex = 0;

	private List<ExcelRow> sheetData;
	
	private Iterator<ExcelRow> sheetDataIterator;

	private OPCPackage pkg;

	private XSSFReader reader;

	private EventHandler handler;

	private Iterator<InputStream> streamIterator;

	private XMLReader parser;
	
	private boolean isEnd = false;
	
	
	//TODO 重载 流
	//TODO 给定sheet
	//TODO HSSF

	public ExcelEventReader(String sourceUri) throws IOException, OpenXML4JException, SAXException {  
		pkg = OPCPackage.open(sourceUri);
		reader = new XSSFReader(pkg);
		streamIterator = reader.getSheetsData();
		handler = new EventHandler(reader.getSharedStringsTable());
		parser = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");
		parser.setContentHandler(handler);
	}

	@Override
	public ReaderRow readRow() throws Exception {
		while(true){
			if(isEnd){
				return null;
			}else if(sheetData == null){
				read();
			}
			
			if(sheetDataIterator.hasNext()){
				return sheetDataIterator.next();
			}else{
				read();
			}
		}
	}


	@Override
	public void close() throws IOException {
		IoUtil.close(pkg);
	}

	private void read() throws Exception {
		if(streamIterator.hasNext()) {
			InputStream sheetStream = null;
			sheetIndex++;
			handler.setSheetIndex(sheetIndex); 
			try{
				sheetStream = streamIterator.next();
				parser.parse(new InputSource(sheetStream));
				sheetData = handler.getSheetData();
				sheetDataIterator = sheetData.iterator();
			}finally{
				IoUtil.close(sheetStream);
			}
		}else{
			isEnd = true;
		}
	}

	public static void main(String[] args) throws Exception {
		ExcelEventReader reader = new ExcelEventReader("D:/1.xlsx");
		ReaderRow row = null;
		while((row = reader.readRow()) != null){
			System.out.println(row);
		}
		reader.close();
	}
}
