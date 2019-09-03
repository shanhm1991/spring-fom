package org.eto.fom.util.file.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.poi.ss.usermodel.DataFormatter;
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
public class ExcelEventReader implements Reader {

	private String type;

	private OPCPackage pkg;

	private ExcelXSSFHandler xssfHandler;

	private POIFSFileSystem pfs;  

	private ExcelHSSFHandler hssfHandler;

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
		//		Iterator<InputStream> it = reader.getSheetsData();
		//		while(it.hasNext()){
		//			InputStream in = it.next();
		//			byte[] buf = new byte[1024];
		//			int len;
		//			while ((len = in.read(buf)) != -1) {
		//				System.out.write(buf, 0, len);
		//			}
		//		}
	}

	@Override
	public ReaderRow readRow() throws Exception {
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

	/**
	 * 
	 * @param sheetRangeIndex sheetRangeIndex
	 * @param sheetName sheetName
	 * @return boolean
	 */
	protected boolean shouldSheetProcess(int sheetRangeIndex, String sheetName) {
		return true;
	}

	/**
	 * sheet重新排序
	 * @param sheetRangeList 原始sheet顺序
	 * @return 重排序后的sheetlist
	 */
	protected List<String> reRangeSheet(List<String> sheetRangeList) {
		return sheetRangeList;
	}

	static enum CellDataType {
		BOOL, ERROR, FORMULA, INLINESTR, SSTINDEX, FORMAT, NULL
	}

	/**
	 * 
	 * @author shanhm
	 *
	 */
	class ExcelXSSFHandler extends DefaultHandler {

		private XSSFReader xssfReader;

		private XMLReader xmlReader;

		private SharedStringsTable stringTable;

		private StylesTable stylesTable;

		//private boolean isInStringTable;

		private List<String> sheetNameList = new ArrayList<>();

		private Map<String, String> sheetNameRidMap = new LinkedHashMap<>();

		private int rowMax;

		private int cellMax;

		private int sheetIndex = 0;

		private int rowIndex = 0;

		private int cellIndex = 0;

		private CellDataType dataType = CellDataType.NULL;

		private DataFormatter formatter = new DataFormatter();

		private String formatString;

		private short formatIndex;

		private String lastContents = ""; 

		private List<String> columnList;

		private boolean isEnd = false;

		private List<ExcelRow> sheetData;

		private Iterator<ExcelRow> sheetDataIterator;

		public ExcelXSSFHandler(XSSFReader xssfReader, XMLReader xmlReader) throws InvalidFormatException, IOException, SAXException, DocumentException {  
			this.xmlReader = xmlReader;
			this.xssfReader = xssfReader;
			this.stringTable = xssfReader.getSharedStringsTable();
			this.stylesTable = xssfReader.getStylesTable();
			InputStream bookStream = null;
			try{
				bookStream = xssfReader.getWorkbookData();
				//xmlReader.parse(new InputSource(bookStream));没有startElement事件，只好这里自定义解析
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
			sheetNameList = ExcelEventReader.this.reRangeSheet(sheetNameList);
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
					ExcelRow data = new ExcelRow(rowIndex, new ArrayList<String>(0));
					data.setSheetIndex(sheetIndex); 
					data.setSheetName(sheetNameList.get(sheetIndex)); 
					data.setEmpty(true); 
					data.setLastRow(rowIndex == rowMax - 1); 
					sheetData.add(data);
					rowIndex++;
				}
				columnList = new ArrayList<>(cellMax);
			}

			if(qName.equals("c")) {
				int currentCellIndex = getCellIndex(attributes.getValue("r"));
				while(cellIndex < currentCellIndex){
					columnList.add("");
					cellIndex++;
				}

				String cellType = attributes.getValue("t");
				if ("b".equals(cellType)) {
					dataType = CellDataType.BOOL;
				} else if ("e".equals(cellType)) {
					dataType = CellDataType.ERROR;
				} else if ("inlineStr".equals(cellType)) {
					dataType = CellDataType.INLINESTR;
				} else if ("s".equals(cellType)) {
					dataType = CellDataType.SSTINDEX;
				} else if ("str".equals(cellType)) {
					dataType = CellDataType.FORMULA;
				}

				String cellStyle = attributes.getValue("s");
				if (cellStyle != null) {
					dataType = CellDataType.FORMAT;
					int styleIndex = Integer.parseInt(cellStyle);
					XSSFCellStyle style = stylesTable.getStyleAt(styleIndex);
					formatString = style.getDataFormatString();
					formatIndex = style.getDataFormat();
					if (formatString == null) {
						formatString = BuiltinFormats.getBuiltinFormat(formatIndex);
					}
				}
			}
		}

		@SuppressWarnings("deprecation")
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equals("v")) {
				System.out.println(formatString); 
				switch (dataType){
				case SSTINDEX:
					int sharedIndex = Integer.valueOf(lastContents);
					lastContents = new XSSFRichTextString(stringTable.getEntryAt(sharedIndex)).toString();
					break;
				case FORMAT:
					if (formatString != null) {
						lastContents = formatter.formatRawCellContents(
								Double.parseDouble(lastContents), formatIndex, formatString).trim();
					}
					break;
				default:

				}
			}

			if (qName.equals("c")) {
				columnList.add(lastContents);
				lastContents = "";
				cellIndex++;
			}

			if (qName.equals("row")) {
				ExcelRow data = new ExcelRow(rowIndex, columnList);
				data.setSheetIndex(sheetIndex); 
				data.setSheetName(sheetNameList.get(sheetIndex)); 
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
					c++;//相当于26进制，个位A当做0，进位A当做1
					for(int j = 0; j < bitIndex; j++){
						base *=  26;
					}
				}
				cellIndex += c * base;
				bitIndex++;
			}
			return cellIndex;
		}

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

		private void read() throws Exception {
			if(sheetIndex < sheetNameList.size()) {
				String sheetName = sheetNameList.get(sheetIndex);
				if(!shouldSheetProcess(sheetIndex, sheetName)){
					sheetIndex++;
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
				sheetIndex++;
				return;
			}else{
				isEnd = true;
			}
		}
	}

	/**
	 * 
	 * @author shanhm
	 *
	 */
	class ExcelHSSFHandler implements HSSFListener {

		private SSTRecord sstrec;

		private String sheetName;  

		@Override
		public void processRecord(Record record) {

			switch (record.getSid()) {  
			case BOFRecord.sid:  
				BOFRecord bof = (BOFRecord) record;
				if (bof.getType() == BOFRecord.TYPE_WORKBOOK) {
					System.out.println("处理 workbook");
				} else if (bof.getType() == BOFRecord.TYPE_WORKSHEET) {
					System.out.println("处理sheet");
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
				// SSTRecords store a array of unique strings used in Excel.
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

		public ReaderRow readRow() throws Exception {

			return null;
		}
	}
}
