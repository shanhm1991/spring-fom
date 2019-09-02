package org.eto.fom.util.file.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.eto.fom.util.IoUtil;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * 
 * @author shanhm
 *
 */
public class SAXReader implements Reader {
	
	private OPCPackage pkg;
	
	public SAXReader(String sourceUri) throws InvalidFormatException { 
		pkg = OPCPackage.open(sourceUri);
	}

	@Override
	public ReaderRow readRow() throws Exception {
		return null;
	}


	@Override
	public void close() throws IOException {
		IoUtil.close(pkg);
	}

	private void read(String filename) throws Exception {
		OPCPackage pkg =null;
		InputStream sheetStream=null;
		try{
			pkg = OPCPackage.open(filename);
			XSSFReader reader = new XSSFReader(pkg); //TODO 
			SAXHandler handler = new SAXHandler(reader.getSharedStringsTable());
			
			XMLReader parser = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");
			parser.setContentHandler(handler);

			Iterator<InputStream> it = reader.getSheetsData();
			while(it.hasNext()) {
				sheetStream = it.next();
				InputSource sheetSource = new InputSource(sheetStream);
				parser.parse(sheetSource);
			}
		}finally{
			IoUtil.close(pkg);
			IoUtil.close(sheetStream);
		}
	}
}
