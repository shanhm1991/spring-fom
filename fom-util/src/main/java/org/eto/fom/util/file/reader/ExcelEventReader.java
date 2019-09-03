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

import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.hssf.eventusermodel.EventWorkbookBuilder.SheetRecordCollectingListener;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingCellDummyRecord;
import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
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

	private void initHssf(){

	}

	private void initXssf() throws IOException, OpenXML4JException, SAXException, DocumentException {  
		XSSFReader reader = new XSSFReader(pkg);
		XMLReader parser = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");
		xssfHandler = new ExcelXSSFHandler(reader, parser);
		parser.setContentHandler(xssfHandler);
	}

	@Override
	public ReaderRow readRow() throws Exception {
		if(EXCEL_XLS.equalsIgnoreCase(type)){
			return null;//TODO
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

	/**
	 * 
	 * @author shanhm
	 *
	 */
	private class ExcelXSSFHandler extends DefaultHandler {

		private XSSFReader xssfReader;

		private XMLReader xmlReader;

		private SharedStringsTable stringTable;

		private boolean isInStringTable;

		private List<String> sheetNameList = new ArrayList<>();

		private Map<String, String> sheetNameRidMap = new LinkedHashMap<>();

		private int sheetIndex = 0;

		private int rowIndex = 0;

		private int cellIndex = 0;

		private int rowMax;

		private int cellMax;

		private String lastContents = ""; 

		private List<String> columnList;

		private boolean isEnd = false;

		private List<ExcelRow> sheetData;

		private Iterator<ExcelRow> sheetDataIterator;

		public ExcelXSSFHandler(XSSFReader xssfReader, XMLReader xmlReader) throws InvalidFormatException, IOException, SAXException, DocumentException {  
			this.xmlReader = xmlReader;
			this.xssfReader = xssfReader;
			this.stringTable = xssfReader.getSharedStringsTable();
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
				isInStringTable = "s".equals(attributes.getValue("t"));
			}
		}

		@SuppressWarnings("deprecation")
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equals("v")) {
				if (isInStringTable) {
					int sharedIndex = Integer.valueOf(lastContents);
					lastContents = new XSSFRichTextString(stringTable.getEntryAt(sharedIndex)).toString();
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
	private class ExcelHSSFHandler implements HSSFListener {

		private FormatTrackingHSSFListener formatListener;  

		private SheetRecordCollectingListener workbookBuildingListener; 

		private boolean outputFormulaValues = true;  
		
		private ArrayList boundSheetRecords = new ArrayList();  
		
		private HSSFWorkbook stubWorkbook;  
		
		private SSTRecord sstRecord;  
		
		private int sheetIndex = -1;  

		private BoundSheetRecord[] orderedBSRs;  
		
		private int nextRow;  

		private int nextColumn;  

		private boolean outputNextStringRecord; 
		
		private int minColumns = -1;  

		private int lastRowNumber;  

		private int lastColumnNumber;  
		
		// 当前行  
		private int curRow = 0;  

		// 存储行记录的容器  
		private List<String> rowlist = new ArrayList<String>();;  

		@SuppressWarnings("unused")  
		private String sheetName;  

		public ExcelHSSFHandler() throws IOException {
			MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(this);  
			formatListener = new FormatTrackingHSSFListener(listener);  
			HSSFEventFactory factory = new HSSFEventFactory();  
			HSSFRequest request = new HSSFRequest();  
			if (outputFormulaValues) {  
				request.addListenerForAllRecords(formatListener);  
			} else {  
				workbookBuildingListener = new SheetRecordCollectingListener(formatListener);  
				request.addListenerForAllRecords(workbookBuildingListener);  
			}  
			factory.processWorkbookEvents(request, pfs);  
		}

		@Override
		public void processRecord(Record record) {
			int thisRow = -1;  
			int thisColumn = -1;  
			String thisStr = null;  
			String value = null;  
			switch (record.getSid()) {  
			case BoundSheetRecord.sid:  
				boundSheetRecords.add(record);  
				break;  
			case BOFRecord.sid:  
				BOFRecord br = (BOFRecord) record;  
				if (br.getType() == BOFRecord.TYPE_WORKSHEET) {  
					// 如果有需要，则建立子工作薄  
					if (workbookBuildingListener != null && stubWorkbook == null) {  
						stubWorkbook = workbookBuildingListener.getStubHSSFWorkbook();  
					}  

					sheetIndex++;  
					if (orderedBSRs == null) {  
						orderedBSRs = BoundSheetRecord.orderByBofPosition(boundSheetRecords);  
					}  
					sheetName = orderedBSRs[sheetIndex].getSheetname();  
				}  
				break;  

			case SSTRecord.sid:  
				sstRecord = (SSTRecord) record;  
				break;  

			case BlankRecord.sid:  
				BlankRecord brec = (BlankRecord) record;  
				thisRow = brec.getRow();  
				thisColumn = brec.getColumn();  
				thisStr = "";  
				rowlist.add(thisColumn, thisStr);  
				break;  
			case BoolErrRecord.sid: // 单元格为布尔类型  
				BoolErrRecord berec = (BoolErrRecord) record;  
				thisRow = berec.getRow();  
				thisColumn = berec.getColumn();  
				thisStr = berec.getBooleanValue() + "";  
				rowlist.add(thisColumn, thisStr);  
				break;  

			case FormulaRecord.sid: // 单元格为公式类型  
				FormulaRecord frec = (FormulaRecord) record;  
				thisRow = frec.getRow();  
				thisColumn = frec.getColumn();  
				if (outputFormulaValues) {  
					if (Double.isNaN(frec.getValue())) {  
						// Formula result is a string  
						// This is stored in the next record  
						outputNextStringRecord = true;  
						nextRow = frec.getRow();  
						nextColumn = frec.getColumn();  
					} else {  
						thisStr = formatListener.formatNumberDateCell(frec);  
					}  
				} else {  
					thisStr = '"' + HSSFFormulaParser.toFormulaString(stubWorkbook, frec.getParsedExpression()) + '"';  
				}  
				rowlist.add(thisColumn, thisStr);  
				break;  
			case StringRecord.sid:// 单元格中公式的字符串  
				if (outputNextStringRecord) {  
					// String for formula  
					StringRecord srec = (StringRecord) record;  
					thisStr = srec.getString();  
					thisRow = nextRow;  
					thisColumn = nextColumn;  
					outputNextStringRecord = false;  
				}  
				break;  
			case LabelRecord.sid:  
				LabelRecord lrec = (LabelRecord) record;  
				curRow = thisRow = lrec.getRow();  
				thisColumn = lrec.getColumn();  
				value = lrec.getValue().trim();  
				value = value.equals("") ? " " : value;  
				this.rowlist.add(thisColumn, value);  
				break;  
			case LabelSSTRecord.sid: // 单元格为字符串类型  
				LabelSSTRecord lsrec = (LabelSSTRecord) record;  
				curRow = thisRow = lsrec.getRow();  
				thisColumn = lsrec.getColumn();  
				if (sstRecord == null) {  
					rowlist.add(thisColumn, " ");  
				} else {  
					value = sstRecord.getString(lsrec.getSSTIndex()).toString().trim();  
					value = value.equals("") ? " " : value;  
					rowlist.add(thisColumn, value);  
				}  
				break;  
			case NumberRecord.sid: // 单元格为数字类型  
				NumberRecord numrec = (NumberRecord) record;  
				curRow = thisRow = numrec.getRow();  
				thisColumn = numrec.getColumn();  
				value = formatListener.formatNumberDateCell(numrec).trim();  
				value = value.equals("") ? " " : value;  
				// 向容器加入列值  
				rowlist.add(thisColumn, value);  
				break;  
			default:  
				break;  
			}  

			// 遇到新行的操作  
			if (thisRow != -1 && thisRow != lastRowNumber) {  
				lastColumnNumber = -1;  
			}  

			// 空值的操作  
			if (record instanceof MissingCellDummyRecord) {  
				MissingCellDummyRecord mc = (MissingCellDummyRecord) record;  
				curRow = thisRow = mc.getRow();  
				thisColumn = mc.getColumn();  
				rowlist.add(thisColumn, " ");  
			}  

			// 更新行和列的值  
			if (thisRow > -1)  
				lastRowNumber = thisRow;  
			if (thisColumn > -1)  
				lastColumnNumber = thisColumn;  

			// 行结束时的操作  
			if (record instanceof LastCellOfRowDummyRecord) {  
				if (minColumns > 0) {  
					// 列值重新置空  
					if (lastColumnNumber == -1) {  
						lastColumnNumber = 0;  
					}  
				}  
				lastColumnNumber = -1;  

				// 每行结束时， 调用getRows() 方法  
				//rowReader.getRows(sheetIndex, curRow, rowlist);  
				// 清空容器  
				rowlist.clear();  
			}  
		}  

	}
}
