package org.eto.fom.task.parse;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eto.fom.util.PatternUtil;
import org.eto.fom.util.file.ZipUtil;
import org.eto.fom.util.file.reader.ExcelRow;
import org.eto.fom.util.file.reader.IExcelReader;

/**
 * 根据配置规则按sheet解析excel
 * 
 * @author shanhm
 * 
 */
public abstract class ParseSheetTask extends ParseExcelTask<Map<String, Object>> {

	private static final int WAIT_FILE = 100;

	//<sheetName, <columnIndex, columName>>
	private Map<String, LinkedHashMap<Integer,String>> columnIndexMap = new HashMap<>();

	//<sheetName, <columnName, <key, value>>>
	private Map<String, Map<String, Map<String,String>>> sheetRule = new LinkedHashMap<>();

	private Set<String> necessarySheet = new HashSet<>();

	//handlerMap
	private Map<String, SheetHandler> sheetHandlerMap = new HashMap<>();

	//excelData
	private final Map<String, Object> excelData = new HashMap<>();

	private File excel;

	private String excelRule;

	protected final String sourceName;

	protected final String sourceUri;

	protected File zipWorkHome;


	public ParseSheetTask(File file, String excelRule) {
		super(file.getPath(), 0, true);
		this.sourceUri = file.getPath();
		this.sourceName = file.getName();
		this.excelRule = excelRule;
	}

	/**
	 * 加载解析规则以及其他配置，解压zip找到需要处理的Excel
	 */
	@Override
	protected boolean beforeExec() throws Exception {

		waitFileComplete();

		excel = findExcel();

		initExcelRule();

		initSheetHander(sheetHandlerMap);

		return true;
	}

	private void waitFileComplete() throws InterruptedException{
		File file = new File(sourceUri);
		long len = file.length();
		Thread.sleep(WAIT_FILE);
		while(len != file.length()){
			Thread.sleep(WAIT_FILE);
		}
	}

	protected File findExcel() throws Exception{
		if(!ZipUtil.valid(sourceUri)){ 
			log.error("invalid zip."); 
			throw new IllegalArgumentException("非法zip.");
		}

		this.zipWorkHome = new File(System.getProperty("cache.parse")
				+ File.separator + getContextName() + File.separator + sourceName);
		if(!zipWorkHome.mkdirs()){
			log.error("directory create failed: {}", zipWorkHome);
			throw new IllegalArgumentException("解压目录创建失败," + zipWorkHome);
		}

		File zip = new File(sourceUri); 
		long cost = ZipUtil.unZip(zip, zipWorkHome);
		String size = formatSize(zip.length());
		log.info("finish unzip({}KB), cost={}ms", size, cost);

		File[] array = zipWorkHome.listFiles(new FileFilter(){
			@Override
			public boolean accept(File file) {
				String name = file.getName().toLowerCase();
				return name.endsWith(IExcelReader.EXCEL_XLSX) || name.endsWith(IExcelReader.EXCEL_XLS);
			}
		});
		if(array == null || array.length != 1  ){
			throw new IllegalArgumentException("zip文件不合法，Excel(.xls或.xlsx)文件有且仅能有一个)");
		}
		return array[0];
	}

	protected abstract void initSheetHander(Map<String, SheetHandler> handlerMap);

	protected void initExcelRule() throws DocumentException, FileNotFoundException {  
		String excelRulePath = getContextConfig().getString(excelRule, "");
		if(excelRulePath == null){
			throw new IllegalArgumentException("未发现excel解析规则.");
		}
		File ruleXml = new File(excelRulePath);
		if(!ruleXml.exists()){
			throw new IllegalArgumentException("未发现excel解析规则.");
		}

		SAXReader reader = new SAXReader();
		reader.setEncoding("UTF-8");
		Document doc = reader.read(new FileInputStream(ruleXml));
		Element root = doc.getRootElement();
		for(Object o : root.elements("sheet")){
			Element sheet = (Element)o;
			String sheetName = sheet.attributeValue("name");
			if(StringUtils.isBlank(sheetName)){
				continue;
			}
			Map<String, Map<String,String>> indexMap = new HashMap<>();
			for(Object obj : sheet.elements("column")){
				Element column = (Element)obj;
				String columnName = column.attributeValue("name");
				String fileld = column.attributeValue("field");
				if(!StringUtils.isBlank(fileld) && !StringUtils.isBlank(columnName)){ 
					Map<String,String> fieldMap = new HashMap<>();
					fieldMap.put("field", fileld);
					fieldMap.put("type", column.attributeValue("type"));
					fieldMap.put("pattern", column.attributeValue("pattern"));
					fieldMap.put("default", column.attributeValue("default"));
					fieldMap.put("notnull", column.attributeValue("notnull"));
					fieldMap.put("unnecessary", column.attributeValue("unnecessary"));
					indexMap.put(columnName, fieldMap);
				}
			}
			sheetRule.put(sheetName, indexMap);

			String unnecessary = sheet.attributeValue("unnecessary");
			if(!"true".equals(unnecessary)){
				necessarySheet.add(sheetName);
			}
		}
	}

	@Override
	protected boolean afterExec(Boolean execResult) throws Exception {
		clean();
		return true;
	}

	protected void clean() throws IOException { 
		if(zipWorkHome != null && zipWorkHome.exists()){ 
			File[] fileArray = zipWorkHome.listFiles();
			if(!ArrayUtils.isEmpty(fileArray)){
				for(File file : fileArray){
					if(!Files.deleteIfExists(file.toPath())){
						log.warn("clear temp file failed: " + file.getName()); 
					}
				}
			}
			if(!Files.deleteIfExists(zipWorkHome.toPath())){
				log.warn("clear temp directory failed."); 
			}
		}
	}

	@Override
	protected InputStream getExcelInputStream(String sourceUri) throws Exception {
		return new FileInputStream(excel);
	}

	@Override
	protected String getExcelType() {
		String name = excel.getName();
		return name.substring(name.lastIndexOf('.') + 1, name.length());
	}

	@Override
	protected long getSourceSize(String sourceUri) {
		return excel.length();
	}

	/**
	 * 以配置的解析规则为准，存在几种情况
	 * 1. Excel有值列，但不在配置规则中
	 * 2. 规则有配置,但Excel没有对应列，不过配置unnecessary="true",同时有默认值,此时取默认值
	 * 3. 规则有配置,Excel也有对应列，但当读取目标行时没有这一列（通常在行尾），此时如果有配置默认值，则取默认值
	 */
	@Override
	protected List<Map<String, Object>> parseRowData(ExcelRow row, long batchTime) throws Exception {
		String sheet = row.getSheetName();
		List<String> columns = row.getColumnList();

		Map<String,Object> data = new HashMap<>();
		data.put("sheet", sheet);
		data.put("row", row.getRowIndex());//在数据中补上Excel信息，在后续数据层反馈数据错误时方便定位

		Map<String, Map<String,String>> rowRule = sheetRule.get(sheet);//not null(filter)
		if(row.getRowIndex() == 0){
			LinkedHashMap<Integer,String> link = new LinkedHashMap<>();
			for(int i = 0;i < columns.size();i++){
				link.put(i, columns.get(i).trim());
			}
			isDataLack(sheet, link, rowRule);
			columnIndexMap.put(sheet, link);
			return Arrays.asList(data); 
		}

		LinkedHashMap<Integer,String> indexMap = columnIndexMap.get(sheet);//not null
		for(Entry<Integer,String> entry : indexMap.entrySet()){
			int index = entry.getKey();
			String columnName = entry.getValue();//not null
			String columnValue = null;
			//3. 规则有配置,Excel也有对应列，但当读取目标行时没有这一列（通常在行尾），此时如果有配置默认值，则取默认值
			if(columns.size() > index){
				columnValue = columns.get(index).trim();
			}

			Map<String,String> columnRule = rowRule.get(columnName);
			//1. Excel有值列，但不在配置规则中
			if(columnRule == null){
				continue; 
			}
			String field = columnRule.get("field");
			String type = columnRule.get("type");
			String pattern = columnRule.get("pattern");
			String defaultValue = columnRule.get("default");
			String notNull = columnRule.get("notnull");
			if(StringUtils.isBlank(columnValue)){
				if(!StringUtils.isBlank(defaultValue)){
					columnValue = defaultValue;
				}else if("true".equals(notNull)){ 
					throw new IllegalArgumentException(
							buildError("字段不能为空", sheet, row.getRowIndex(), columnName));
				}
			}

			if(!PatternUtil.match(pattern, columnValue)){
				throw new IllegalArgumentException(
						buildError("数据验证失败", sheet, row.getRowIndex(), columnName));
			}
			parseValue(columnValue, type, field, data, columnName);
		}

		//2. 规则有配置,但Excel没有对应列，不过配置unnecessary="true",同时有默认值,此时取默认值
		for(Entry<String, Map<String,String>> entry : rowRule.entrySet()){
			if(indexMap.containsValue(entry.getKey())){
				continue;
			}
			Map<String,String> map = entry.getValue();
			String defaultValue = map.get("default");
			if(StringUtils.isBlank(defaultValue)){
				continue;
			}
			String field = map.get("field");
			String type = map.get("type");
			String pattern = map.get("pattern");
			String notNull = map.get("notnull"); 
			if("true".equals(notNull)){ 
				throw new IllegalArgumentException(
						buildError("字段不能为空", sheet, row.getRowIndex(), entry.getKey()));
			}
			if(!PatternUtil.match(pattern, defaultValue)){
				throw new IllegalArgumentException(
						buildError("数据验证失败", sheet, row.getRowIndex(), entry.getKey()));
			}
			parseValue(defaultValue, type, field, data, entry.getKey());
		}

		return Arrays.asList(data);
	}

	protected void isDataLack(String sheetName, Map<Integer,String> indexMap, Map<String, Map<String,String>> rule){
		for(Entry<String, Map<String,String>> entry : rule.entrySet()){
			String column = entry.getKey();
			String unnecessary = entry.getValue().get("unnecessary");
			if("true".equals(unnecessary)){
				continue;
			}
			if(!indexMap.containsValue(column)){ 
				throw new IllegalArgumentException("缺少column数据:" + column + ", sheet=" + sheetName);
			}
		}
	}

	protected void parseValue(String value, String type, String field, Map<String,Object> data, String columnName){
		if(StringUtils.isBlank(value)){ 
			return;
		}
		data.put(field, value);
	}

	private String buildError(String msg, Object sheet, Object row, String column){
		StringBuilder builder = new StringBuilder(msg);
		builder.append(", sheet=").append(sheet);
		builder.append(", row=").append(row);
		builder.append(", column=").append(column);
		return builder.toString();
	}

	@Override
	protected final void batchProcess(List<Map<String, Object>> sheetData, long batchTime) throws Exception {
		//not empty
		String sheet = sheetData.remove(0).get("sheet").toString();
		SheetHandler handler = sheetHandlerMap.get(sheet);
		if(handler != null){
			handler.handler(sheet, sheetData, batchTime, excelData);
		}else{
			List<Map<String, Object>> list = new ArrayList<>(sheetData.size());
			list.addAll(sheetData);
			excelData.put(sheet, list);
		}
	}

	@Override
	protected final void onExcelComplete(String sourceUri, String sourceName) throws Exception {
		Set<String> parsedSheet = excelData.keySet();
		List<String> list = new ArrayList<>(necessarySheet.size());
		for(String name : necessarySheet){
			if(!parsedSheet.contains(name)){
				list.add(name);
			}
		}
		if(!list.isEmpty()){
			throw new IllegalArgumentException("缺少sheet数据:" + list);  
		}

		columnIndexMap = null;
		sheetRule = null;
		necessarySheet = null;
		sheetHandlerMap = null;
		handlerData(excelData);
	}

	protected abstract void handlerData(Map<String, Object> excelData);

	@Override
	protected boolean sheetFilter(int sheetIndex, String sheetName) { 
		return sheetRule.containsKey(sheetName);
	}

	/**
	 * 
	 * excel的sheet处理
	 * 
	 * @author shanhm
	 * 
	 */
	public static interface SheetHandler {

		/**
		 * 将sheet的数据解析成对应的数据集合，并放入excel数据中
		 * @param sheet sheet名称
		 * @param sheetData sheet数据
		 * @param batchTime 处理时间
		 * @param excelData excel各个sheet的数据集，这地方用Map<String, Object>有点妥协，只是为了方便
		 * @throws Exception
		 */
		void handler(String sheet, List<Map<String, Object>> sheetData, long batchTime, Map<String, Object> excelData) throws Exception;
	}
}
