package com.fom.modules.importer.demo.hm;
import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import com.fom.context.ZipImporter;
import com.fom.util.MD5Util;
import com.fom.util.StorageUtil;
import com.fom.util.db.handler.EsHandler;
import com.fom.util.db.handler.OraHandler;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
public class HMImporter extends ZipImporter<HMConfig, Map<String,Object>> {
	
	private static final String POOL_ES = "zwdata";
	
	private static final String POOL_ORA = "scloudpf";

	private static final String ZIPFLAG_GROUPCHA = "GROUPCHA";

	private static final String INDEXFILE_NODE = "/MESSAGE/DATASET/DATA/DATASET/DATA/DATASET[@name='WA_COMMON_010015']/DATA";

	private static final String SQL = "SELECT A.CMDID, B.LABELID FROM "
			+ "NCPB_COMMAND_MONITOR A, NCPB_RULE_LABEL B WHERE A.BUSINESSID = B.RULEID";

	private static final Map<String,String> ESINDEX_GROUP = new HashMap<String,String>();

	private static final Map<String,String> ESINDEX_GZH = new HashMap<String,String>();

	static{
		ESINDEX_GROUP.put("OPID", "H010005"); //OPID
		ESINDEX_GROUP.put("MSGID", "H010042"); //id
		ESINDEX_GROUP.put("SENDUSERID", "B040003"); //用户ID
		ESINDEX_GROUP.put("RECEIVERID", "D010010"); //群ID
		ESINDEX_GROUP.put("GROUPTYPE", "D010029"); //接收者ID
		ESINDEX_GROUP.put("SENDIPID", "F020016"); //发送IPID
		ESINDEX_GROUP.put("CONTENT", "H040001"); //消息内容
		ESINDEX_GROUP.put("MAINFILE", "H010020"); //文件名
		ESINDEX_GROUP.put("SENDAPPID", "B040002"); //发送者账号
		ESINDEX_GROUP.put("FILEMD5", "WXY0002"); //文件MD5值
		ESINDEX_GROUP.put("KEYWORDS", "WXY0001"); //命中关键字

		ESINDEX_GZH.put("SENDAPPID", "D010013"); //发送者账号
		ESINDEX_GZH.put("SENDUSERID", "B040003"); //发送者ID
		ESINDEX_GZH.put("CAPTURETIME", "H010014"); //发送时间
		ESINDEX_GZH.put("TITLE", "H020002"); //文章标题 
		ESINDEX_GZH.put("ABSTRACT", "G010004"); //摘要
		ESINDEX_GZH.put("CONTENT", "H040001"); //消息内容
		ESINDEX_GZH.put("MAINFILE", "H010020"); //文件名
		ESINDEX_GZH.put("KEYWORDS", "WXY0001"); //命中关键字
		ESINDEX_GZH.put("OPID", "H010005"); //OPID
	}

	private String index_today;

	private List<String> fieldList = new ArrayList<String>();

	private Map<String, Set<String>> labelIdMap = new HashMap<String, Set<String>>();

	protected HMImporter(String name, String path) {
		super(name, path);
	}

	@Override
	protected void beforeExecute(HMConfig config) throws Exception {
		prepareIndex(config);
		prepareLabelIdMap();
	}

	@Override
	protected boolean validZipContent(HMConfig config, List<String> nameList) {
		int xmlCount = 0;
		int bcpCount = 0;
		String xmlName = "";
		for(String name : nameList){
			if(name.endsWith(".xml")){
				xmlName = name;
				xmlCount++;
			}else if(name.endsWith(".bcp")){
				bcpCount++;
			}
		}
		if(bcpCount != 1){
			log.warn("文件校验失败, bcp文件有且仅能有一个");
			return false;
		}

		if(xmlCount != 1){
			log.warn("文件校验失败, xml索引文件有且仅能有一个");
			return false;
		}

		try{
			praseXml(new File(unzipDir + File.separator + xmlName));
		}catch(Exception e){
			log.warn("文件校验失败, xml索引文件解析异常", e);
			return false;
		}
		return true;
	}

	@Override
	protected void praseLineData(HMConfig config, List<Map<String,Object>> lineDatas, String line, long batchTime) throws Exception {
		if (srcName.contains(ZIPFLAG_GROUPCHA)) {
			parseGroupLine(lineDatas, line, config.getZkAddress());
		}else{
			parseGzhLine(lineDatas, line, config.getZkAddress());
		}
	}

	@Override
	protected void batchProcessLineData(HMConfig config, List<Map<String,Object>> lineDatas, long batchTime) throws Exception{ 
		//合并数据：多行数据有可能是同一个msgId,但是只有KEYWORDS不同
		Map<String,Set<String>> keyWords = new HashMap<String,Set<String>>();
		Map<String,Map<String,Object>> mergeData = new HashMap<String,Map<String,Object>>();
		for(Map<String,Object> map : lineDatas){
			String msgId = String.valueOf(map.get("MSGID"));
			Map<String,Object> data = mergeData.get(msgId);
			if(data == null){
				data = map;
				data.put("TAGS", labelIdMap.get(String.valueOf((map.remove("OPID")))));
				mergeData.put(msgId, data);
			}

			String keywords = String.valueOf(map.get("KEYWORDS"));
			if(StringUtils.isNotBlank(keywords)){
				Set<String> set = keyWords.get(msgId);
				if(set == null){
					set = new HashSet<String>();
					keyWords.put(msgId, set);
				}
				set.add(keywords);
			}
		}

		for(Entry<String,Map<String,Object>> entry : mergeData.entrySet()){
			String msgId = entry.getKey();
			Map<String,Object> map = entry.getValue();
			map.put("KEYWORDS", StringUtils.join(keyWords.get(msgId), " "));
		}

		if(mergeData.isEmpty()){
			return;
		}

		EsHandler.defaultHandler.bulkInsert(POOL_ES, index_today, config.getEsType(), mergeData);
		log.info("批处理结束[" + lineDatas.size() + "], 耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
	}

	private void prepareIndex(HMConfig config) throws Exception{
		DateFormat format = new SimpleDateFormat("yyyyMMdd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -21);
		String index_last = config.getEsIndex() + format.format(cal.getTime());
		EsHandler.defaultHandler.synDelIndex(POOL_ES, index_last); 

		index_today = config.getEsIndex() + format.format(System.currentTimeMillis());
		String type = config.getEsType();
		if(EsHandler.defaultHandler.synCreateIndex(POOL_ES, index_today, type, config.getEsJsonFile())){
			log.info("创建ES索引[index=" + index_today + ", type=" + type + "]");
		}
	}

	private void prepareLabelIdMap() throws Exception{ 
		List<Map<String, Object>> list =  OraHandler.defaultHandler.queryForList(POOL_ORA, SQL, null);
		for(Map<String, Object> map : list){
			String cmdId = String.valueOf(map.get("CMDID"));
			String labId = String.valueOf(map.get("LABELID"));
			Set<String> set = labelIdMap.get(cmdId);
			if(set == null){
				set = new HashSet<String>();
				labelIdMap.put(cmdId, set);
			}
			set.add(labId);
		}
	}

	private void praseXml(File xml) throws Exception { 
		SAXReader reader = new SAXReader();
		Document doc = reader.read(new FileInputStream(xml));
		Node dataNode = doc.selectSingleNode(INDEXFILE_NODE);
		if (dataNode.getNodeType() != Element.ELEMENT_NODE) {
			throw new Exception("非法xml索引文件,NodeType不对");
		}
		Element index_doc = (Element) dataNode;
		@SuppressWarnings("unchecked")
		Iterator<Element> elementIterator = index_doc.elementIterator();
		while (elementIterator.hasNext()) {
			Element item = elementIterator.next();
			fieldList.add(item.attribute("key").getText());
		}
	}

	public static void main(String[] args) {

		String md5UserId = MD5Util.getMD5String("1449674680");

		System.out.println(md5UserId);
	}

	private void parseGroupLine(List<Map<String,Object>> lineDatas, String line, String zkAddress) throws Exception { 
		String[] array = line.split("\t");
		Long fileSize = 0L;
		int index_fileSize = fieldList.indexOf("H010021");
		if(index_fileSize != -1 && array.length >= index_fileSize + 1){
			try{
				//fileSize = Long.valueOf(array[index_fileSize]);
				fileSize = StringUtils.isBlank(array[index_fileSize])?0:Long.valueOf(array[index_fileSize]);

			}catch(Exception e){
				log.warn("FILESIZE获取异常", e); 
			}
		}

		Long captureTime = 0L;
		int index_captureTime = fieldList.indexOf("H010014");
		if(index_captureTime != -1 && array.length >= index_captureTime + 1){
			try{
				captureTime = Long.valueOf(array[index_captureTime]);
			}catch(Exception e){
				log.warn("CAPTURETIME获取异常", e); 
			}
		}

		Long sendIp = 0L;
		int index_sendIp = fieldList.indexOf("F020004");
		if(index_sendIp != -1 && array.length >= index_sendIp + 1){
			try{
				sendIp = Long.valueOf(array[index_sendIp]);
			}catch(Exception e){
				log.warn("SENDIP获取异常", e); 
			}
		}

		String sendIpId = "";
		int index_sendIpId = fieldList.indexOf("F020016");
		if(index_sendIpId != -1 && array.length >= index_sendIpId + 1){
			sendIpId = array[index_sendIpId];
		}
		String sendip_country = "";// 国家编码
		String sendip_province = ""; // 省／直辖市编码
		String sendip_city = ""; // 地市编码
		String sendip_region = ""; // 区域编码
		if(StringUtils.isNotBlank(sendIpId)){
			sendip_country = sendIpId.substring(0, 4);
			sendip_province =  sendIpId.substring(0, 6);
			sendip_city = sendIpId.substring(0, 8);
			sendip_region=sendIpId.substring(0, 10);
		}

		Integer msgType = 0;
		int index_msgType = fieldList.indexOf("J050021");
		if(index_msgType != -1 && array.length >= index_msgType + 1){
			try{
				msgType = Integer.valueOf(array[index_msgType]);
			}catch(Exception e){
				log.warn("MSGTYPE获取异常", e); 
			}
		}

		String md5UserId = null;
		int index_userId = fieldList.indexOf("B040003");
		if(index_userId != -1 && array.length >= index_userId + 1){
			md5UserId = MD5Util.getMD5String(String.valueOf(array[index_userId]));
		}

		String md5groupId = null;
		int index_groupId = fieldList.indexOf("D010010");
		if(index_groupId != -1 && array.length >= index_groupId + 1){
			md5groupId = MD5Util.getMD5String(String.valueOf(array[index_groupId]));
		}


		Map<String,Object> map = new HashMap<String,Object>();
		for(Entry<String,String> entry : ESINDEX_GROUP.entrySet()){
			int index = fieldList.indexOf(entry.getValue());
			if(index != -1 && array.length >= index + 1){
				map.put(entry.getKey(), array[index]);
			}
		}

		map.put("SENDIP", sendIp);
		map.put("APPTYPE",1030036); //信息来源应用网安编码
		map.put("DATATYPE", 103001); //数据类型
		map.put("SENDIP_COUNTRY", sendip_country);
		map.put("SENDIP_PROVINCE", sendip_province);
		map.put("SENDIP_CITY", sendip_city);
		map.put("SENDIP_REGION", sendip_region);
		map.put("ARTICLEURL", "");
		map.put("TITLE", "");
		map.put("ABSTRACT", "");
		map.put("RECEIVERACCOUNT", ""); //接收者账号
		map.put("CAPTURETIME", captureTime);
		map.put("FILESIZE", fileSize);
		map.put("MSGTYPE", msgType);
		map.put("FROMDATA", 1);
		map.put("GROUPID_MD5", md5groupId);
		map.put("USERID_MD5", md5UserId);
		String mainFileName = String.valueOf(map.get("MAINFILE"));
		File file = new File(unzipDir + File.separator + mainFileName);
		String url = "";
		if(file.exists()){
			url = StorageUtil.storageFile(file, zkAddress, true);
		}
		map.put("MEDIAURL", url); 
		lineDatas.add(map);
	} 

	private void parseGzhLine(List<Map<String,Object>> lineDatas, String line, String zkAddress) throws Exception { 
		String[] array = line.split("\t");

		Long sendIp = 0L;
		int index_sendIp = fieldList.indexOf("F020004");
		if(index_sendIp != -1 && array.length >= index_sendIp + 1){
			try{
				sendIp = Long.valueOf(array[index_sendIp]);
			}catch(Exception e){
				log.warn("SENDIP获取异常", e); 
			}
		}

		Integer msgType = 0;
		int index_msgType = fieldList.indexOf("H010002");
		if(index_msgType != -1 && array.length >= index_msgType + 1){
			try{
				msgType = Integer.valueOf(array[index_msgType]);
			}catch(Exception e){
				log.warn("MSGTYPE获取异常", e); 
			}
		}

		Map<String,Object> map = new HashMap<String,Object>();
		for(Entry<String,String> entry : ESINDEX_GZH.entrySet()){
			int index = fieldList.indexOf(entry.getValue());
			if(index != -1 && array.length >= index + 1){
				map.put(entry.getKey(), array[index]);
			}
		}
		map.put("SENDIP", sendIp);
		map.put("APPTYPE", 1030036); //信息来源应用网安编码
		map.put("DATATYPE", 103001); //数据类型
		map.put("SENDIP_COUNTRY", "");
		map.put("SENDIP_PROVINCE", "");
		map.put("SENDIP_CITY", "");
		map.put("SENDIP_REGION", "");
		map.put("ARTICLEURL", "");
		map.put("RECEIVERID", ""); //接收者 公众号填空
		map.put("RECEIVERACCOUNT", ""); //接收者账号 填空
		map.put("MSGTYPE",msgType);
		map.put("FROMDATA", 1);

		String mainFileName = String.valueOf(map.get("MAINFILE"));
		File file = new File(unzipDir + File.separator + mainFileName);
		String url = "";
		if(file.exists()){
			url = StorageUtil.storageFile(file, zkAddress, true);
		}
		map.put("MEDIAURL", url); 
		lineDatas.add(map);
	}
}
