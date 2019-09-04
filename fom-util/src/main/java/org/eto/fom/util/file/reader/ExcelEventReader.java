package org.eto.fom.util.file.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RowRecord;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eto.fom.util.IoUtil;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * 
 * @author shanhm
 *
 */
public class ExcelEventReader implements IReader {

	private static final Logger LOG = Logger.getLogger(ExcelEventReader.class);

	private String type;

	private OPCPackage pkg;

	private ExcelXSSFHandler xssfHandler;

	private POIFSFileSystem pfs;  

	private ExcelHSSFHandler hssfHandler;
	
	private ExcelSheetFilter sheetFilter;
	
	/**
	 * @param sheetFilter
	 */
	public void setSheetFilter(ExcelSheetFilter sheetFilter) {
		this.sheetFilter = sheetFilter;
	}

	/**
	 * set the sheet list and order to be readed by sheetIndex
	 * @param indexList start from 1
	 */
	public void setSheetListForReadByIndex(List<Integer> indexList){
		List<String> nameList = new ArrayList<>();
		for(int index : indexList){
			if(index > xssfHandler.sheetNameList.size()){
				continue;
			}
			nameList.add(xssfHandler.sheetNameList.get(index - 1));
		}
		xssfHandler.sheetNameGivenList = nameList;
	}

	/**
	 * set the sheet list and order to be readed by sheetName
	 * @param nameList
	 */
	public void setSheetListForReadByName(List<String> nameList){
		xssfHandler.sheetNameGivenList = nameList;
	}

	public ExcelEventReader(String sourceUri) throws IOException, OpenXML4JException, SAXException, DocumentException {  
		int index = sourceUri.lastIndexOf('.');
		if(index == -1){
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx.");
		}
		this.type = sourceUri.substring(index + 1); 
		if(EXCEL_XLS.equalsIgnoreCase(type)){
			pfs = new POIFSFileSystem(new FileInputStream(sourceUri));  
			initHssf();
		}else if(EXCEL_XLSX.equalsIgnoreCase(type)){
			pkg = OPCPackage.open(sourceUri); 
			initXssf();
		}else{
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx");
		}
	}

	public ExcelEventReader(File file) throws IOException, OpenXML4JException, SAXException, DocumentException { 
		String name = file.getName();
		int index = name.lastIndexOf('.');
		if(index == -1){
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx");
		}
		this.type = name.substring(index + 1);
		if(EXCEL_XLS.equalsIgnoreCase(type)){
			pfs = new POIFSFileSystem(new FileInputStream(file));  
			initHssf();
		}else if(EXCEL_XLSX.equalsIgnoreCase(type)){
			pkg = OPCPackage.open(file); 
			initXssf();
		}else{
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx");
		}
	}

	public ExcelEventReader(InputStream inputStream, String type) throws IOException, OpenXML4JException, SAXException, DocumentException { 
		this.type = type;
		if(EXCEL_XLS.equalsIgnoreCase(type)){
			pfs = new POIFSFileSystem(inputStream);  
			initHssf();
		}else if(EXCEL_XLSX.equalsIgnoreCase(type)){
			pkg = OPCPackage.open(inputStream); 
			initXssf();
		}else{
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx");
		}
	} 

	private void initHssf() throws IOException {
		hssfHandler = new ExcelHSSFHandler();
		HSSFRequest hssfRequest = new HSSFRequest();
		hssfRequest.addListenerForAllRecords(hssfHandler);
		HSSFEventFactory factory = new HSSFEventFactory(); 
		InputStream bookStream = pfs.createDocumentInputStream("Workbook");
		factory.processEvents(hssfRequest, bookStream);
	}

	private void initXssf() throws IOException, OpenXML4JException, SAXException, DocumentException {  
		XSSFReader reader = new XSSFReader(pkg);
		XMLReader parser = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");
		xssfHandler = new ExcelXSSFHandler(reader, parser);
		parser.setContentHandler(xssfHandler);
	}

	@Override
	public ExcelRow readRow() throws Exception {
		if(EXCEL_XLS.equalsIgnoreCase(type)){
			return hssfHandler.readRow();
		}else if(EXCEL_XLSX.equalsIgnoreCase(type)){
			return xssfHandler.readRow();
		}else{
			throw new UnsupportedOperationException("Excel file name must end with .xls or .xlsx");
		}
	}

	@Override
	public void close() throws IOException {
		IoUtil.close(pkg);
		IoUtil.close(pfs);
	}
	
	class ExcelXSSFHandler extends DefaultHandler {

		private XSSFReader xssfReader;

		private XMLReader xmlReader;

		private SharedStringsTable stringTable;

		private StylesTable stylesTable;
		
		private Map<String, String> sheetNameRidMap = new LinkedHashMap<>();

		private List<String> sheetNameList = new ArrayList<>();

		private List<String> sheetNameGivenList = new ArrayList<>();

		private int sheetIndexReading = 0;

		private String sheetName;

		private int sheetIndex;

		private int rowMax;

		private int cellMax;

		private int rowIndex = 0;

		private int cellIndex = 0;

		private CellType cellType;

		private String lastContents = ""; 

		private List<String> columnList;

		private boolean isEnd = false;

		private List<ExcelRow> sheetData;

		private Iterator<ExcelRow> sheetDataIterator;

		private DataFormatter formatter = new DataFormatter();

		private String formatString;

		private short formatIndex;

		public ExcelXSSFHandler(XSSFReader xssfReader, XMLReader xmlReader) throws InvalidFormatException, IOException, SAXException, DocumentException {  
			this.xmlReader = xmlReader;
			this.xssfReader = xssfReader;
			this.stringTable = xssfReader.getSharedStringsTable();
			this.stylesTable = xssfReader.getStylesTable();
			InputStream bookStream = null;
			try{
				bookStream = xssfReader.getWorkbookData();
				SAXReader reader = new SAXReader();
				reader.setEncoding("UTF-8");
				Document doc = reader.read(bookStream);
				Element book = doc.getRootElement();
				Element sheets = book.element("sheets");
				Iterator<?> it = sheets.elementIterator("sheet");
				while(it.hasNext()){
					Element sheet = (Element)it.next();
					String name = sheet.attributeValue("name");
					String rid = sheet.attributeValue("id");
					sheetNameList.add(name);
					sheetNameRidMap.put(name, rid); 
				}
			}finally{
				IoUtil.close(bookStream); 
			}
			//cann't let the customer code to directly modify sheetNameList
			sheetNameGivenList.addAll(sheetNameList);
			if(sheetFilter != null){
				sheetFilter.resetSheetListForRead(sheetNameGivenList);
			}
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equals("dimension")) {
				String dimension = attributes.getValue("ref");
				if(dimension.contains(":")){
					dimension = dimension.split(":")[1];
				}
				rowMax = getRowIndex(dimension);  
				cellMax = getCellIndex(dimension);
				sheetData = new ArrayList<>(rowMax);
			}

			if (qName.equals("row")) { 
				cellIndex = 0;
				int currentRowIndex = Integer.valueOf(attributes.getValue("r")) - 1;
				while(rowIndex < currentRowIndex){
					ExcelRow data = new ExcelRow(rowIndex + 1, new ArrayList<String>(0));
					data.setSheetIndex(sheetIndex); 
					data.setSheetName(sheetName); 
					data.setEmpty(true); 
					data.setLastRow(rowIndex == rowMax - 1); 
					sheetData.add(data);
					rowIndex++;
				}
				columnList = new ArrayList<>(cellMax);
			}

			if(qName.equals("c")) {
				lastContents = "";
				int currentCellIndex = getCellIndex(attributes.getValue("r"));
				while(cellIndex < currentCellIndex){
					columnList.add("");
					cellIndex++;
				}

				String type = attributes.getValue("t");
				if(type == null){
					cellType = CellType.BLANK;
				}else{
					switch(type){
					case "b":
						cellType = CellType.BOOLEAN; break;
					case "e":
						cellType = CellType.ERROR; break;
					case "inlineStr":
						//just use CellType.NUMERIC to represent inlineStr
						cellType = CellType.NUMERIC; break; 
					case "s":
						cellType = CellType.STRING; break;
					case "str":
						cellType = CellType.FORMULA; break;
					default:
						cellType = CellType.BLANK;
					}
				}

				String cellStyle = attributes.getValue("s");
				if (cellStyle != null) {
					int styleIndex = Integer.parseInt(cellStyle);
					XSSFCellStyle style = stylesTable.getStyleAt(styleIndex);
					formatIndex = style.getDataFormat();
					formatString = style.getDataFormatString();
					if (formatString == null) {
						formatString = BuiltinFormats.getBuiltinFormat(formatIndex);
					}
				}
			}
		}

		@SuppressWarnings("deprecation")
		public void endElement(String uri, String localName, String qName) throws SAXException {
			/**
			 * All fields are uniformly read into string,
			 * and the date type will be first converted to milliseconds.
			 */
			if (qName.equals("v")) {
				switch (cellType){
				case STRING:
					int sharedIndex = Integer.valueOf(lastContents);
					lastContents = new XSSFRichTextString(stringTable.getEntryAt(sharedIndex)).toString();
					break;
				case NUMERIC:
					lastContents = new XSSFRichTextString(lastContents).toString();
					break;
				case BOOLEAN:
				case ERROR:
				case FORMULA:
				default:

				}

				if (formatString != null) {
					try{
						double value = Double.valueOf(lastContents);
						if(DateUtil.isADateFormat(formatIndex,formatString)) {
							if(DateUtil.isValidExcelDate(value)) {
								Date date = DateUtil.getJavaDate(value, false);
								lastContents = String.valueOf(date.getTime());
							}
						}else{
							lastContents = formatter.formatRawCellContents(value, formatIndex, formatString).trim();
						}
					}catch(Exception e){
						LOG.error("Excel format error: sheetName=" + sheetName + ", rowIndex=" + (rowIndex + 1),  e);
					}
				}
			}

			if (qName.equals("c")) {
				columnList.add(lastContents);
				cellIndex++;
			}

			if (qName.equals("row")) {
				ExcelRow data = new ExcelRow(rowIndex + 1, columnList);
				data.setSheetIndex(sheetIndex); 
				data.setSheetName(sheetName); 
				data.setEmpty(false); 
				data.setLastRow(rowIndex == rowMax - 1); 
				sheetData.add(data);
				rowIndex++;
			}
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			lastContents += new String(ch, start, length);
		}

		private int getRowIndex(String dimension){
			int len = dimension.length();
			int index = 0;
			for(int i = len - 1; i >= 0; i--){
				if(dimension.charAt(i) > 64){//A
					index = i;
					break;
				}
			}
			return Integer.valueOf(dimension.substring(index + 1));
		}

		private int getCellIndex(String dimension){
			int len = dimension.length();
			int index = 0;
			for(int i = len - 1; i >= 0; i--){
				if(dimension.charAt(i) > 64){//A
					index = i;
					break;
				}
			}
			String cellstr = dimension.substring(0, index + 1);

			int cellIndex = 0;
			int indexLen = cellstr.length();
			int bitIndex = 0;
			for(int i = indexLen - 1; i >= 0; i--){ 
				int base = 1;
				int c = cellstr.charAt(i) - 65;
				if(bitIndex > 0){
					c++;//number of columns used 26 jinzhi
					for(int j = 0; j < bitIndex; j++){
						base *=  26;
					}
				}
				cellIndex += c * base;
				bitIndex++;
			}
			return cellIndex;
		}

		public ExcelRow readRow() throws Exception {
			while(true){
				if(isEnd){
					return null;
				}else if(sheetData == null){
					read();
					continue;
				}

				if(sheetDataIterator.hasNext()){
					return sheetDataIterator.next();
				}else{
					read();
				}
			}
		}

		private void read() throws Exception {
			if(sheetIndexReading < sheetNameGivenList.size()) {
				sheetName = sheetNameGivenList.get(sheetIndexReading);
				sheetIndex = sheetNameList.indexOf(sheetName) + 1;
				if(sheetIndex == -1){
					sheetIndexReading++;
					return;
				}

				if(sheetFilter != null && !sheetFilter.filter(sheetIndex, sheetName)){
					sheetIndexReading++;
					return;
				}

				String relId = sheetNameRidMap.get(sheetName);
				rowIndex = 0;
				InputStream sheetStream = null;
				try{
					sheetStream = xssfReader.getSheet(relId);
					xmlReader.parse(new InputSource(sheetStream));
					sheetDataIterator = sheetData.iterator();
				}finally{
					IoUtil.close(sheetStream);
				}
				sheetIndexReading++;
			}else{
				isEnd = true;
			}
		}
	}

	class ExcelHSSFHandler implements HSSFListener {

		private SSTRecord sstrec;

		private String sheetName;  

		@Override
		public void processRecord(Record record) {

			switch (record.getSid()) {  
			case BOFRecord.sid:  
				BOFRecord bof = (BOFRecord) record;
				if (bof.getType() == BOFRecord.TYPE_WORKBOOK) {

				} else if (bof.getType() == BOFRecord.TYPE_WORKSHEET) {

				}
				break;  
			case BoundSheetRecord.sid:
				BoundSheetRecord bsr = (BoundSheetRecord) record;
				sheetName = bsr.getSheetname();
				System.out.println(sheetName); 
				break;
			case RowRecord.sid:
				RowRecord rowrec = (RowRecord) record;
				System.out.println("Row found, first column at "
						+ rowrec.getFirstCol() + " last column at " + rowrec.getLastCol());
				break;
			case NumberRecord.sid:
				NumberRecord numrec = (NumberRecord) record;
				System.out.println("Cell found with value " + numrec.getValue()
				+ " at row " + numrec.getRow() + " and column " + numrec.getColumn());
				break;
			case SSTRecord.sid:
				sstrec = (SSTRecord) record;
				for (int k = 0; k < sstrec.getNumUniqueStrings(); k++) {
					System.out.println("String table value " + k + " = " + sstrec.getString(k));
				}
				break;
			case LabelSSTRecord.sid:
				LabelSSTRecord lrec = (LabelSSTRecord) record;
				System.out.println("String cell found with value "
						+ sstrec.getString(lrec.getSSTIndex()));
				break;
			}  

		}

		public ExcelRow readRow() throws Exception {

			return null;
		}
	}

}

