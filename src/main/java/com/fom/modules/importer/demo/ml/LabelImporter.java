package com.fom.modules.importer.demo.ml;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.elasticsearch.action.bulk.BulkItemResponse;

import com.fom.context.ZipImporter;
import com.fom.util.MD5Util;
import com.fom.util.db.handler.EsHandler;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
public class LabelImporter extends ZipImporter<LabelImporterConfig, Map<String,Object>>{
	
	private static final String POOL_ES = "zwdata";

	private final String Tag; 

	private Random random = new Random();

	private int retryTimes = 0;

	protected LabelImporter(String name, String path) throws Exception {
		super(name, path);
		this.Tag = srcName.split("_")[0];
	}

	@Override
	protected void executeBefore(LabelImporterConfig config) throws Exception{
		if(EsHandler.defaultHandler.synCreateIndex(POOL_ES, config.getEsIndex(), config.getEsType(), config.getEsJsonFile())){
			log.info("创建ES索引[index=" + config.getEsIndex() + ", type=" + config.getEsType() + "]");
		}
	}
	
	@Override
	protected void praseLineData(LabelImporterConfig config, List<Map<String, Object>> lineDatas, String line, long batchTime) throws Exception {
		Map<String,Object> map = new HashMap<String,Object>();
		try{
			if("WXY00106".equals(Tag)){
				String[] filedValue = line.trim().replaceAll("\\[", "").replaceAll("\\]", "").split("\t");
				map.put("DOCID", "1030036_" + filedValue[0]);
				map.put("GROUPID", filedValue[0]);

				long mili = new Date().getTime();
				map.put("FTIME", Long.valueOf(mili/1000));
				map.put("LTIME", Long.valueOf(mili/1000));

				String[] wordArray = filedValue[1].split(" ");
				String[] hitArray = filedValue[2].split(",");
				List<String> wordList = new ArrayList<String>();
				List<Integer> hitList = new ArrayList<Integer>();
				for(int i = 0;i < wordArray.length;i++){
					wordList.add(wordArray[i]);
					hitList.add(Integer.valueOf(hitArray[i].trim()));
				}
				map.put("HIT_KEYWORDS_106",wordList);
				map.put("HIT_TIMES_106", hitList);
				map.put("HIT_WORD_COUNTS_106",filedValue[4]);

				map.put(Tag, Long.valueOf(String.valueOf(filedValue[3]))); //TOTAL_HIT_TIMES
			}else{
				Object[] filedValue = line.trim().split("\t");
				List<String> fileds = config.getFiledList();
				Map<String,String> patternMap = config.getPatternMap();
				for(String filed : fileds){
					String pattern = patternMap.get(filed);
					
					
					
					map.put(filed, MessageFormat.format(pattern, filedValue));
				}
				
				//有可能异常
				map.put("FTIME", Long.valueOf(String.valueOf(map.get("FTIME"))));
				map.put("LTIME", Long.valueOf(String.valueOf(map.get("LTIME"))));
				map.put(Tag, Long.valueOf(map.get("HIT_TIMES").toString()));
			}
		}catch(Exception e){
			log.warn("忽略行数据：" + line + "," + e.getMessage());
			return;
		}
		
		List<String> ls = new ArrayList<String>();
		ls.add(Tag);
		map.put("TAGS", ls);
		
		if(map.containsKey("USERID")){
			String md5_userid = MD5Util.getMD5String(String.valueOf(map.get("USERID")));
			map.put("USERID_MD5", md5_userid);
		}else if(map.containsKey("GROUPID")){
			String md5_userid = MD5Util.getMD5String(String.valueOf(map.get("GROUPID")));
			map.put("GROUPID_MD5", md5_userid);
		}
		lineDatas.add(map);
	}

	@Override
	protected void batchProcessLineData(LabelImporterConfig config, List<Map<String, Object>> lineDatas, long batchTime) throws Exception {
		Map<String,Map<String,Object>> insertData = new HashMap<String, Map<String,Object>>((int)(lineDatas.size() / 0.75));
		//Map<DOCID,Map<filed,filedValue>> 合并源数据的计数和最早最晚时间
		for(Map<String, Object> map : lineDatas){
			String docId = String.valueOf(map.get("DOCID"));
			Map<String,Object> data = insertData.get(docId);
			if(data == null){
				insertData.put(docId, map);
				continue;
			}
			long count1 = (Long)map.get(Tag);
			long count2 = (Long)data.get(Tag);
			data.put(Tag, count1 + count2);
			mergeTime(docId, data, map);
		}

		//合并修改提交远程库
		List<Map<String, Object>> remoteData = 
				EsHandler.defaultHandler.multiGet(POOL_ES, config.getEsIndex(), config.getEsType(), insertData.keySet());
		Map<String,Map<String,Object>> localData = new HashMap<String, Map<String,Object>>();
		for(Map<String, Object> eMap : remoteData){
			String id = eMap.keySet().iterator().next();
			localData.put(id, insertData.remove(id));
		}
		
		Map<String,Map<String,Object>> updateData = buildUpdateData(remoteData, localData);
		Set<BulkItemResponse> conflictSet = 
				EsHandler.defaultHandler.bulkUpdate(POOL_ES, config.getEsIndex(), config.getEsType(), updateData);
		EsHandler.defaultHandler.bulkInsert(POOL_ES, config.getEsIndex(), config.getEsType(), insertData);
		log.info("批处理结束[" + lineDatas.size() + "], inser=" + insertData.size() + ", update=" + localData.size() 
		+ ",updateFailed=" + conflictSet.size() + ", 耗时=" + (System.currentTimeMillis() - batchTime) + "ms"); 

		resolveConflict(POOL_ES, config.getEsIndex(), config.getEsType(), conflictSet, localData);
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

	//与库中的数据进行比较合并
	private Map<String,Map<String,Object>> buildUpdateData(List<Map<String, Object>> remoteData, Map<String,Map<String,Object>> localData) {
		Map<String,Map<String,Object>> updateData = new HashMap<>((int)(remoteData.size() / 0.75));
		for(Map<String,Object> remoteMap : remoteData){
			String docId = String.valueOf(remoteMap.get("DOCID"));
			Map<String,Object> localMap = localData.get(docId);
			if(localMap == null){//should never happened
				continue;
			}

			mergeTime(docId, localMap, remoteMap);
			Map<String,Object> updateMap = new HashMap<>();
			updateMap.put("FTIME", localMap.get("FTIME"));
			updateMap.put("LTIME", localMap.get("LTIME"));

			if("WXY00106".equals(Tag) && remoteMap.get("HIT_KEYWORDS_106") != null){
				merge106KetWord(docId, localMap, remoteMap, updateMap);
			}

			mergeTags(docId, localMap, remoteMap, updateMap);

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

	//WXY00106 合并 HIT_KEYWORDS_106 HIT_TIMES_106 HIT_WORD_COUNTS_106，这个合并并不准，暂时不做要求
	@SuppressWarnings("unchecked")
	private void merge106KetWord(String docId, Map<String,Object> localMap, Map<String,Object> remoteMap, Map<String,Object> updateMap){
		try{
			ArrayList<String> keyWords = (ArrayList<String>)remoteMap.get("HIT_KEYWORDS_106");
			ArrayList<Integer> keyHits = (ArrayList<Integer>)remoteMap.get("HIT_TIMES_106");
			Map<String,Integer> map106 = new HashMap<String,Integer>();
			for(int i = 0;i < keyWords.size();i++){
				map106.put(keyWords.get(i), keyHits.get(i)); 
			}

			ArrayList<String> n_keyWords = (ArrayList<String>)localMap.get("HIT_KEYWORDS_106");
			ArrayList<Integer> n_keyHits = (ArrayList<Integer>)localMap.get("HIT_TIMES_106");
			Map<String,Integer> newmap106 = new HashMap<String,Integer>();
			for(int i = 0;i < n_keyWords.size();i++){
				newmap106.put(n_keyWords.get(i), n_keyHits.get(i)); 
			}

			//合并
			for(Entry<String,Integer> entry : map106.entrySet()){
				String existKey = entry.getKey();
				Integer newValue = newmap106.get(existKey);
				if(newValue != null){
					map106.put(existKey,entry.getValue() + newValue);//map106大小不变
					newmap106.remove(existKey);
				}
			}
			map106.putAll(newmap106); 

			List<String> wordList = new ArrayList<String>();
			wordList.addAll(map106.keySet());

			List<Integer> hitList = new ArrayList<Integer>();
			for(String word : wordList){
				hitList.add(map106.get(word));
			}

			updateMap.put("HIT_KEYWORDS_106", wordList);
			updateMap.put("HIT_TIMES_106", hitList);
			updateMap.put("HIT_WORD_COUNTS_106", wordList.size());
		}catch(Exception e){
			log.warn("[id=" + docId + "]合并00106关键词异常", e); 
		}
	}

	//合并TAGS
	@SuppressWarnings("unchecked")
	private void mergeTags(String docId, Map<String,Object> localMap, Map<String,Object> remoteMap, Map<String,Object> updateMap){
		Object tags = remoteMap.get("TAGS");
		List<String> tagList = null;
		if(tags != null){
			tagList = (List<String>)remoteMap.get("TAGS");
		}else{
			tagList = new ArrayList<String>();
		}

		if(!tagList.contains(Tag)){
			tagList.add(Tag);
		}
		updateMap.put("TAGS", tagList);

		//合并Tag计数，可能出现错误情况TAGS中没有Tag，但确有Tag计数的情况，应该分开独立合并
		long newCount = (Long)localMap.get(Tag);//validLine保证一定是long
		updateMap.put(Tag, newCount);
		if(null != remoteMap.get(Tag)){
			String existTagCount = String.valueOf(remoteMap.get(Tag));
			try{
				updateMap.put(Tag, newCount + Long.valueOf(existTagCount));
			}catch(NumberFormatException e){
				log.warn("[id=" + docId + "]修复错误数据," + Tag + ":" + existTagCount + "->" + newCount); 
			}
		}
	}

	
}
