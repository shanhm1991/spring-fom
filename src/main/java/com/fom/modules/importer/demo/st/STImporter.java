package com.fom.modules.importer.demo.st;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.elasticsearch.action.bulk.BulkItemResponse;

import com.fom.context.ZipImporter;
import com.fom.util.MD5Util;
import com.fom.util.StorageUtil;
import com.fom.util.db.handler.EsHandler;
import com.fom.util.db.handler.OraHandler;

/**
 * 可疑目标
 */
public class STImporter extends ZipImporter<STConfig, Map<String, String>> {
	
	private static final String POOL_ES = "zwdata";
	
	private static final String POOL_ORA_scloudrs = "scloudrs";
	
	private static final String POOL_ORA_scloudpf = "scloudpf";

	private static final String INDEXFILE_NODE = "/MESSAGE/DATASET/DATA/DATASET/DATA/DATASET[@name='WA_COMMON_010015']/DATA";

	private static final String ZIPFLAG_YG = "YJMONITOR";

	private static final String ZIPFLAG_GROUP = "GROUP";

	private static final String ZIPFLAG_GZH = "GZH";

	private static final String SQL_LABEL = "SELECT RULEID, LABELID FROM NCPB_RULE_LABEL";

	private static final String SQL = "insert into NCRS_SUSPICIOUS_ORIGINDATA("
			+ "LABELID, APPTYPE, RESOURCETYPE, SENDUSERID, SENDAPPID,"
			+ "SENDNICKNAME, SENDREMARKNAME, RECEIVERID, RECEIVERTYPE, RECEIVERACCOUNT,"
			+ "RECEIVERNICKNAME, CAPTURETIME, MEDIATYPE, SENDIP, SENDPORT,"
			+ "TRANSFILE, MAINFILE, FILESIZE, SENDIPID, SENDAREACODE,"
			+ "CONTENT, CONTENTURL, MSGID, LIKECNT, DURATION,"
			+ "STARTPOSITION, COMMENTCNT, INSERTTIME )values(#LABELID#, '1030036', '', #SENDUSERID#, #SENDAPPID#,"
			+ "'', '', #RECEIVERID#, '', '','', #CAPTURETIME#, '', #SENDIP#, 0,"
			+ "#TRANSFILE#, #MAINFILE#, '', #SENDIPID#, #SENDAREACODE#,#CONTENT#, '', #MSGID#, 0, 0,0, 0, #INSERTTIME#)";

	private List<String> fieldList = new ArrayList<String>(); 

	private Map<String, String> labelMap = new HashMap<String, String>();

	private String Tag_YJ;//远鉴文件的标签打在zip文件上,一个zip文件只有一个标签，而group和gzh的数据的标签在bcp的行数据中

	private Random random = new Random();

	private int retryTimes = 0;

	protected STImporter(String name, String path) {
		super(name, path);
	}

	@Override
	protected void executeBefore(STConfig config) throws Exception{
		prepareIndex(config);
		prepareLabelMap();
	}

	@Override
	protected boolean validZipContent(STConfig config, List<String> nameList) {
		//为了避免出现Tag_YJ获取失败，务必将正则表达式表达精确 ，为了与原设计流程保持一致(清理机制)，没有将这个判断放在prepare中而是在解压之后判断
		//YJMONITOR-5210-440300-440301-1516432270-WZSZ_ICP_1030036_YJMONITOR_RESULT_WXY_1_YBZ99302_20180120094602_004_ZDR_1
		if(srcName.startsWith(ZIPFLAG_YG)) {
			Tag_YJ = srcName.split("-")[5].split("_")[7];
			if(!labelMap.containsKey(Tag_YJ)){
				return false;
			}
		}

		int bcpCount = 0;
		int xmlCount = 0;
		String xmlName = null;
		for(String name : nameList){
			if(name.endsWith(".bcp")){
				bcpCount++;
			}else if(name.endsWith(".xml")){
				xmlCount++;
				xmlName = name; //GAB_ZIP_INDEX.xml
			}
		}
		if(bcpCount != 1){
			log.warn("文件校验失败,bcp文件有且仅能有一个");
			return false;
		}else if(xmlCount != 1){
			log.warn("文件校验失败,xml文件有且仅能有一个");
		}
		try{
			praseXml(unzipDir + File.separator + xmlName);
		}catch(Exception e){
			log.warn("文件校验失败, xml索引文件解析失败");
			return false;
		}
		return true;
	}

	@Override
	protected void praseLineData(STConfig config, List<Map<String, String>> lineDatas, String line, long batchTime) throws Exception {
		String[] array = line.split("\t");
		int fieldNum = array.length;
		if(fieldList.size() < fieldNum){//避免数组越界
			fieldNum = fieldList.size();
		}

		Map<String, String> map = new HashMap<String, String>();
		for(int i = 0; i < fieldNum; i++){
			map.put(fieldList.get(i), array[i]); 
		}

		//去除不必入库的标签数据，YJ的直接在上面根据文件名获取标签判断，在validZipFiles()中判断
		String ruleId = map.get("H010005");
		if(!srcName.startsWith(ZIPFLAG_YG)){
			if(StringUtils.isBlank(ruleId) || !labelMap.containsKey(ruleId)){
				log.warn("忽略数据,标签获取失败[" + ruleId + "]");
				return;
			}
			map.put("H010005", labelMap.get(ruleId));
		}
		lineDatas.add(map);
	}

	@Override
	protected void batchProcessLineData(STConfig config, List<Map<String, String>> lineDatas, long batchTime) throws Exception {
		processOracleData(lineDatas, POOL_ORA_scloudrs, config.getZkAddress());
		if(srcName.indexOf(ZIPFLAG_GZH) != -1){
			processGzhEsData(lineDatas, config);
		}else if(srcName.indexOf(ZIPFLAG_GROUP) != -1){
			processGroupEsData(lineDatas, config);
		}else{
			processYJEsData(lineDatas, config);
		}
	}

	private void prepareIndex(STConfig config) throws Exception{
		if(EsHandler.defaultHandler.synCreateIndex(
				POOL_ES, config.getUserIndex(), config.getUserType(), config.getUserJsonFile())){
			log.info("创建ES索引[index=" + config.getUserIndex() + ", type=" + config.getUserType() + "]");
		}
		if(EsHandler.defaultHandler.synCreateIndex(
				POOL_ES, config.getGroupIndex(), config.getGroupType(), config.getGroupJsonFile())){
			log.info("创建ES索引[index=" + config.getGroupIndex() + ", type=" + config.getGroupType() + "]");
		}
		if(EsHandler.defaultHandler.synCreateIndex(
				POOL_ES, config.getGzhIndex(), config.getGzhType(), config.getGzhJsonFile())){
			log.info("创建ES索引[index=" + config.getGzhIndex() + ", type=" + config.getGzhType() + "]");
		}
	}

	private void prepareLabelMap() throws Exception{ 
		List<Map<String, Object>> list =  
				OraHandler.defaultHandler.queryForList(POOL_ORA_scloudpf, SQL_LABEL, null);
		for(Map<String, Object> map : list){
			String ruleId = String.valueOf(map.get("RULEID"));
			String labelId = String.valueOf(map.get("LABELID"));
			labelMap.put(ruleId, labelId);
			//		labelMap.put("783", "NC000002");
		}
	}

	private void praseXml(String xmlPath) throws Exception {
		SAXReader reader = new SAXReader();
		Document doc = reader.read(new FileInputStream(xmlPath));
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

	private void processOracleData(List<Map<String, String>> lineDatas, String scloudrs, String zkAddress) throws Exception{ 
		long sTime = System.currentTimeMillis();
		List<Map<String, Object>> paramMaps = new ArrayList<Map<String, Object>>();
		long insertTime = sTime / 1000L;
		for(Map<String,String> data : lineDatas){
			Map<String, Object> map = new HashMap<String, Object>();
			try{
				map.put("CAPTURETIME", Long.valueOf(data.get("H010014"))); 
				map.put("SENDIP", Long.valueOf(data.get("F020004"))); 
			}catch(Exception e){
				log.warn("忽略行数据，数据转换失败CAPTURETIME/SENDIP"); 
				continue;
			}

			String mainFileName = String.valueOf(data.get("H010020"));
			map.put("MAINFILE", mainFileName);

			File file = new File(unzipDir + File.separator + mainFileName);
			String url = "";
			if(file.exists()){
				url = StorageUtil.storageFile(file, zkAddress, true);
			}
			map.put("TRANSFILE", url); 

			if(srcName.startsWith(ZIPFLAG_YG)){
				map.put("LABELID", labelMap.get(Tag_YJ)); 
				map.put("SENDUSERID", String.valueOf(data.get("F020016"))); 
			}else{
				map.put("LABELID", String.valueOf(data.get("H010005")));
				map.put("SENDUSERID", String.valueOf(data.get("B040003"))); 
			}

			map.put("MSGID", String.valueOf(data.get("H010042"))); 
			map.put("SENDAPPID", String.valueOf(data.get("B040002"))); 
			map.put("RECEIVERID", String.valueOf(data.get("D010010"))); 
			map.put("SENDIPID", String.valueOf(data.get("F020016"))); 
			map.put("SENDAREACODE", String.valueOf(data.get("B030002"))); 
			map.put("CONTENT", String.valueOf(data.get("H040001"))); 
			map.put("INSERTTIME", insertTime); 
			paramMaps.add(map);
		}
		OraHandler.defaultHandler.batchExecute(scloudrs, SQL, paramMaps); 
		log.info("批处理入库oracle结束[" + lineDatas.size() + "], insert=" + paramMaps.size() + ", 耗时=" + (System.currentTimeMillis() - sTime) + "ms");
	}

	//GZHTAG
	@SuppressWarnings("unchecked")
	private void processGzhEsData(List<Map<String, String>> lineDatas, STConfig config) throws Exception {
		long sTime = System.currentTimeMillis();

		//以docId作为key对tag进行合并
		Map<String,Map<String,Object>> insertData = new HashMap<String, Map<String,Object>>();

		for(Map<String, String> map : lineDatas){
			long userId = 0;
			try{
				userId = Long.valueOf(map.get("B040003"));
			}catch(Exception e){
				continue;
			}

			long fTime = 0;
			try{
				fTime = Long.valueOf(map.get("H010014"));
			}catch(Exception e){
			}

			String docId = "1030036_" + userId;
			Map<String,Object> data = insertData.get(docId);
			if(data == null){
				data = new HashMap<String,Object>();
				data.put("DOCID", docId);
				data.put("APPTYPE", "1030036");
				data.put("USERID", userId);
				data.put("APPID", map.get("D010013"));
				data.put("DecryptAPPID", map.get("D010013"));
				data.put("ACCOUNTNAME", map.get("H020002"));
				data.put("FTIME", fTime); 
				data.put("LTIME", fTime);
				data.put("TAGS", new ArrayList<String>());
				insertData.put(docId, data);
			}
			String tag = map.get("H010005"); //not null
			List<String> tags = (List<String>)data.get("TAGS");
			if(!tags.contains(tag)){
				tags.add(tag);
				data.put(tag, 1L);
			}else{
				long count = (Long)data.get(tag);
				data.put(tag, count + 1);
			}
		}

		//与es库中已存在的数据进行比较合并
		List<Map<String, Object>> remoteData = EsHandler.defaultHandler.multiGet(POOL_ES, config.getGzhIndex(), config.getGzhType(), insertData.keySet());
		Map<String,Map<String,Object>> localData = new HashMap<String, Map<String,Object>>();
		for(Map<String, Object> eMap : remoteData){
			String id = eMap.keySet().iterator().next();
			localData.put(id, insertData.remove(id));
		}
		Map<String,Map<String,Object>> updateData = buildUpdateData(remoteData, localData);

		EsHandler.defaultHandler.bulkInsert(POOL_ES, config.getGzhIndex(), config.getGzhType(), insertData);
		Set<BulkItemResponse> conflictSet = 
				EsHandler.defaultHandler.bulkUpdate(POOL_ES, config.getGzhIndex(), config.getGzhType(), updateData); 
		log.info("ES.gzhTag批处理结束[" + lineDatas.size() + "], inser=" + insertData.size() + ", update=" + localData.size() 
		+ ",conflictFailed=" + conflictSet.size() + ", 耗时=" + (System.currentTimeMillis() - sTime) + "ms"); 

		resolveConflict(POOL_ES, config.getGzhIndex(), config.getGzhType(), conflictSet, localData);
	}

	//递归处理VersionConflictEngineException数据
	private void resolveConflict(String pool, String index, String type, 
			Set<BulkItemResponse> conflictSet, Map<String,Map<String,Object>> localData) throws Exception { 
		if(conflictSet.isEmpty()){
			retryTimes = 0;
			return;
		}

		if(retryTimes >= 100){
			log.error("持续冲突超过100次，丢弃数据：");
			for(BulkItemResponse item : conflictSet){
				log.error(item.getFailureMessage());
			}
			retryTimes = 0;
			return;
		}

		retryTimes++;
		Thread.sleep(100 + random.nextInt(400)); 

		Set<String> idSet = new HashSet<>();
		for(BulkItemResponse item : conflictSet){
			idSet.add(item.getId());
		}

		List<Map<String, Object>> remoteData = EsHandler.defaultHandler.multiGet(pool, index, type, idSet);
		Map<String,Map<String,Object>> updateData = buildUpdateData(remoteData, localData);
		conflictSet = EsHandler.defaultHandler.bulkUpdate(pool, index, type, updateData);
		log.info("处理更新失败数据[conflict], retryTimes=" + retryTimes + "reolve=" + conflictSet + ", stillFailed=" + conflictSet.size()); 

		resolveConflict(pool, index, type, conflictSet, localData);
	}

	//GROUPTAG  USERTAG
	@SuppressWarnings("unchecked")
	private void processGroupEsData(List<Map<String, String>> lineDatas, STConfig config) throws Exception {

		long sTime = System.currentTimeMillis();
		Map<String,Map<String,Object>> userInsertData = new HashMap<String, Map<String,Object>>();
		Map<String,Map<String,Object>> groupInsertData = new HashMap<String, Map<String,Object>>();
		for(Map<String, String> map : lineDatas){
			long fTime = 0;
			try{
				fTime = Long.valueOf(map.get("H010014"));
			}catch(Exception e){
			}

			long userId = 0;
			try{
				userId = Long.valueOf(map.get("B040003"));

			}catch(Exception e){
			}
			if(userId != 0){ 
				String docId = "1030036_" + userId;
				//得到userid的MD5值
				String useridMD5 = MD5Util.getMD5String(String.valueOf(userId));

				Map<String,Object> data = userInsertData.get(docId);
				if(data == null){
					data = new HashMap<String,Object>();
					data.put("DOCID", docId);
					data.put("APPTYPE", "1030036");
					data.put("ACCOUNTNAME", map.get("B040002"));
					data.put("USERID_MD5",useridMD5);
					data.put("USERID", userId);
					data.put("FTIME", fTime);
					data.put("LTIME", fTime);
					data.put("TAGS", new ArrayList<String>());
					userInsertData.put(docId, data);
				}
				String tag = map.get("H010005"); //not null
				List<String> tags = (List<String>)data.get("TAGS");
				if(!tags.contains(tag)){
					tags.add(tag);
					data.put(tag, 1L);
				}else{
					long count = (Long)data.get(tag);
					data.put(tag, count + 1);
				}
			}

			long groupId = 0;
			try{
				groupId = Long.valueOf(map.get("D010010"));
			}catch(Exception e){
			}
			if(groupId != 0){ 
				String docId = "1030036_" + groupId;
				Map<String,Object> data = groupInsertData.get(docId);
				//得到groupId的Md5值
				String groupidMd5 = MD5Util.getMD5String(String.valueOf(groupId));

				if(data == null){
					data = new HashMap<String,Object>();
					data.put("DOCID", docId);
					data.put("APPTYPE", 1030036);
					data.put("GROUPID", groupId);
					data.put("GROUPID_MD5", groupidMd5);
					data.put("FTIME", fTime);
					data.put("LTIME", fTime);
					data.put("TAGS", new ArrayList<String>());
					groupInsertData.put(docId, data);
				}
				String tag = map.get("H010005"); //not null
				List<String> tags = (List<String>)data.get("TAGS");
				if(!tags.contains(tag)){
					tags.add(tag);
					data.put(tag, 1L);
				}else{
					long count = (Long)data.get(tag);
					data.put(tag, count + 1);
				}
			}
		}

		//与es库中已存在的数据进行比较合并
		List<Map<String, Object>> userRemoteData = 
				EsHandler.defaultHandler.multiGet(POOL_ES, config.getUserIndex(), config.getUserType(), userInsertData.keySet());
		Map<String,Map<String,Object>> userLocalData = new HashMap<String, Map<String,Object>>();
		for(Map<String, Object> eMap : userRemoteData){
			String id = eMap.keySet().iterator().next();
			userLocalData.put(id, userInsertData.remove(id));
		}

		Map<String,Map<String,Object>> userUpdateData = buildUpdateData(userRemoteData, userLocalData);
		Set<BulkItemResponse> userConflictSet = 
				EsHandler.defaultHandler.bulkUpdate(POOL_ES, config.getUserIndex(), config.getUserType(), userUpdateData);
		EsHandler.defaultHandler.bulkInsert(POOL_ES, config.getUserIndex(), config.getUserType(), userInsertData);

		log.info("ES.userTag批处理结束, insert=" + userInsertData.size() + ",update=" + userUpdateData.size() 
		+ ",updateFailed=" + userConflictSet.size() + ",耗时=" + (System.currentTimeMillis() - sTime) + "ms"); 
		resolveConflict(POOL_ES, config.getUserIndex(), config.getUserType(), userConflictSet, userLocalData);


		List<Map<String, Object>> groupRemoteData = 
				EsHandler.defaultHandler.multiGet(POOL_ES, config.getGroupIndex(), config.getGroupType(), groupInsertData.keySet()); 
		Map<String,Map<String,Object>> groupLocalData = new HashMap<String, Map<String,Object>>();
		for(Map<String, Object> eMap : groupRemoteData){
			String id = eMap.keySet().iterator().next();
			groupLocalData.put(id, userInsertData.remove(id));
		}

		Map<String,Map<String,Object>> groupUpdateData = buildUpdateData(groupRemoteData, groupLocalData);
		Set<BulkItemResponse> groupConflictSet = 
				EsHandler.defaultHandler.bulkUpdate(POOL_ES, config.getGroupIndex(), config.getGroupType(), groupUpdateData);
		EsHandler.defaultHandler.bulkInsert(POOL_ES, config.getGroupIndex(), config.getGroupType(), groupInsertData);
		log.info("ES.groupTag批处理结束,insert=" + groupInsertData.size() + ",update=" + groupUpdateData.size()
		+ ",updateFailed=" + groupConflictSet.size() + ",耗时=" + (System.currentTimeMillis() - sTime) + "ms");
	}


	private void processYJEsData(List<Map<String, String>> lineDatas, STConfig config) throws Exception {
		long sTime = System.currentTimeMillis();

		Map<String,Map<String,Object>> userInsertData = new HashMap<String, Map<String,Object>>();
		Map<String,Map<String,Object>> groupInsertData = new HashMap<String, Map<String,Object>>();

		for(Map<String, String> map : lineDatas){
			long fTime = 0;
			try{
				fTime = Long.valueOf(map.get("H010014"));
			}catch(Exception e){
			}

			long userId = 0;
			try{
				userId = Long.valueOf(map.get("B040003")); 
			}catch(Exception e){
			}
			if(userId != 0){ 
				String docId = "1030036_" + userId;
				Map<String,Object> data = userInsertData.get(docId);
				//得到userId的Md5值
				String useridMd5 = MD5Util.getMD5String(String.valueOf(userId));
				if(data == null){
					data = new HashMap<String,Object>();        
					data.put("_id", docId);
					data.put("DOCID", docId);
					data.put("APPTYPE", 1030036);
					data.put("USERID", userId);
					data.put("USERID_MD5", useridMd5);
					data.put("ACCOUNTNAME", map.get("B040002"));
					data.put("FTIME", fTime);
					data.put("LTIME", fTime);
					data.put(labelMap.get(Tag_YJ), 0L); //YJ文件的标签打在zip文件名上，固定只有一个
					List<String> tags = new ArrayList<String>(); 
					tags.add(labelMap.get(Tag_YJ));
					data.put("TAGS", labelMap.get(Tag_YJ));
					userInsertData.put(docId, data);
				}
				long count = (Long)data.get(labelMap.get(Tag_YJ));
				data.put(labelMap.get(Tag_YJ), count + 1);
			}

			long groupId = 0;
			try{
				groupId = Long.valueOf(map.get("D010010"));
			}catch(Exception e){
			}
			if(groupId != 0){ 
				String docId = "1030036_" + groupId;
				Map<String,Object> data = groupInsertData.get(docId);
				//获取groupID的Md5值
				String groupidMd5 = MD5Util.getMD5String(String.valueOf(groupId));
				if(data == null){
					data = new HashMap<String,Object>();   
					data.put("_id", docId);
					data.put("DOCID", docId);
					data.put("APPTYPE", 1030036);
					data.put("GROUPID", groupId);
					data.put("GROUPID_MD5", groupidMd5);
					data.put("FTIME", fTime);
					data.put("LTIME", fTime);
					data.put(labelMap.get(Tag_YJ), 0L); 
					List<String> tags = new ArrayList<String>(); 
					tags.add(labelMap.get(Tag_YJ));
					data.put("TAGS", tags);
					groupInsertData.put(docId, data);
				}
				long count = (Long)data.get(labelMap.get(Tag_YJ));
				data.put(labelMap.get(Tag_YJ), count + 1);
			}
		}

		List<Map<String, Object>> userRemoteData = 
				EsHandler.defaultHandler.multiGet(POOL_ES, config.getUserIndex(), config.getUserType(), userInsertData.keySet()); 
		Map<String,Map<String,Object>> userLocalData = new HashMap<String, Map<String,Object>>();
		for(Map<String, Object> eMap : userRemoteData){
			String id = eMap.keySet().iterator().next();
			userLocalData.put(id, userInsertData.remove(id));
		}

		Map<String,Map<String,Object>> userUpdateData = buildUpdateData(userRemoteData, userLocalData);
		Set<BulkItemResponse> userConflictSet = 
				EsHandler.defaultHandler.bulkUpdate(POOL_ES, config.getUserIndex(), config.getUserType(), userUpdateData);
		EsHandler.defaultHandler.bulkInsert(POOL_ES, config.getUserIndex(), config.getUserType(), userInsertData);

		log.info("ES.userTag批处理结束, insert=" + userInsertData.size() + ",update=" + userUpdateData.size() 
		+ ",updateFailed=" + userConflictSet.size() + ",耗时=" + (System.currentTimeMillis() - sTime) + "ms"); 
		resolveConflict(POOL_ES, config.getUserIndex(), config.getUserType(), userConflictSet, userLocalData);


		List<Map<String, Object>> groupRemoteData = 
				EsHandler.defaultHandler.multiGet(POOL_ES, config.getGroupIndex(), config.getGroupType(), groupInsertData.keySet()); 
		Map<String,Map<String,Object>> groupLocalData = new HashMap<String, Map<String,Object>>();
		for(Map<String, Object> eMap : groupRemoteData){
			String id = eMap.keySet().iterator().next();
			groupLocalData.put(id, userInsertData.remove(id));
		}		

		Map<String,Map<String,Object>> groupUpdateData = buildUpdateData(groupRemoteData, groupLocalData);
		Set<BulkItemResponse> groupConflictSet = 
				EsHandler.defaultHandler.bulkUpdate(POOL_ES, config.getGroupIndex(), config.getGroupType(), groupUpdateData);
		EsHandler.defaultHandler.bulkInsert(POOL_ES, config.getGroupIndex(), config.getGroupType(), groupInsertData);
		log.info("ES.groupTag批处理结束,insert=" + groupInsertData.size() + ",update=" + groupUpdateData.size()
		+ ",updateFailed=" + groupConflictSet.size() + ",耗时=" + (System.currentTimeMillis() - sTime) + "ms");
	}

	@SuppressWarnings("unchecked")
	private Map<String,Map<String,Object>> buildUpdateData(List<Map<String, Object>> remoteData, final Map<String,Map<String,Object>> localData) {
		Map<String,Map<String,Object>> updateData = new HashMap<>();
		for(Map<String,Object> remoteMap : remoteData){
			String docId = String.valueOf(remoteMap.get("DOCID"));
			Map<String,Object> localMap = localData.get(docId);
			if(localMap == null){// never should happened；
				continue;
			}

			mergeTime(docId, localMap, remoteMap);
			Map<String,Object> updateMap = new HashMap<>();
			updateMap.put("FTIME", localMap.get("FTIME"));
			updateMap.put("LTIME", localMap.get("LTIME"));

			List<String> updateTags = new ArrayList<>();// 不可以改变localMap中的TAGS
			updateTags.addAll((List<String>)localMap.get("TAGS"));
			updateMap.put("TAGS", updateTags);
			for(String tag : updateTags){
				String count = String.valueOf(localMap.get(tag));
				try{
					updateMap.put(tag, Long.valueOf(count));
				}catch(Exception e){
					log.error("[Tag=" + tag + "]计数错误：" + count);
				}
			}

			//合并TAGS
			List<String> remoteTags = (List<String>)remoteMap.get("TAGS");
			if(remoteTags != null){
				for(String tag : remoteTags){
					if(!updateTags.contains(tag)){ 
						updateTags.add(tag);
						continue;
					}
					try{
						updateMap.put(tag, Long.valueOf(String.valueOf(remoteMap.get(tag))) + (long)updateMap.get(tag));
					}catch(Exception e){
						continue;
					}
				}
			}
			updateData.put(docId, updateMap);
		}
		return updateData;
	}

	private void mergeTime(String docId, Map<String,Object> localMap, Map<String,Object> remoteMap){
		String nf = String.valueOf(localMap.get("FTIME"));
		String nl = String.valueOf(localMap.get("LTIME"));
		long nFTIME = 0;
		long nLTIME = 0;
		try{
			nFTIME = Long.valueOf(nf);
			nLTIME = Long.valueOf(nl);
		}catch(Exception e){
			log.warn("[id=" + docId + "]更新时间失败,错误数据FTIME:" + nf + ", LTIME:" + nl);
			return;
		}

		//更新FTIME LTIME，如果已存数据有异常就覆盖
		String of = String.valueOf(remoteMap.get("FTIME"));
		String ol = String.valueOf(remoteMap.get("LTIME"));
		long eFTIME = 0;
		long eLTIME = 0;
		try{
			eFTIME = Long.valueOf(of);
			eLTIME = Long.valueOf(ol);
		}catch(Exception e){
			localMap.put("FTIME", nFTIME);
			localMap.put("LTIME", nLTIME);
			log.warn("[id=" + docId + "]修复错误数据,FTIME:" + of + "->" + nf + ", LTIME:" + ol + "->" + nl); 
			return;
		}

		if(eFTIME > nFTIME){
			localMap.put("FTIME", nFTIME);
		}
		if(eLTIME < nLTIME){
			localMap.put("LTIME", nLTIME);
		}
	}
}
