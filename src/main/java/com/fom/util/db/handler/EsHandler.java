package com.fom.util.db.handler;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.bulk.BulkItemResponse;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
public interface EsHandler {
	
	EsHandler defaultHandler = new EsHandlerImpl();

	/**
	 * 同步判断新建索引
	 * @param poolName
	 * @param index
	 * @param type
	 * @param jsonFile
	 * @throws Exception
	 */
	boolean synCreateIndex(String poolName, String index, String type, File jsonFile) throws Exception;

	/**
	 * 同步删除索引
	 * @param poolName
	 * @param index
	 * @throws Exception
	 */
	void synDelIndex(String poolName, String index) throws Exception;

	/**
	 * 批量查询,根据主键
	 * @param poolName
	 * @param index
	 * @param type
	 * @param keySet
	 * @return
	 * @throws Exception
	 */
	List<Map<String,Object>> multiGet(String poolName, String index, String type, Set<String> keySet) throws Exception;

	/**
	 * 批量更新
	 * @param poolName
	 * @param index
	 * @param type
	 * @param data
	 * @return 由于VersionConflictEngineException而失败的数据
	 * @throws Exception
	 */
	Set<BulkItemResponse> bulkUpdate(String poolName, String index, String type, Map<String,Map<String,Object>> data) throws Exception;

	/**
	 * 批量新增
	 * @param poolName
	 * @param index
	 * @param type
	 * @param data
	 * @throws Exception
	 */
	void bulkInsert(String poolName, String index, String type, Map<String,Map<String,Object>> data) throws Exception;

	/**
	 * 批量删除
	 * @param poolName
	 * @param index
	 * @param type
	 * @param keySet
	 * @throws Exception
	 */
	void blukDelete(String poolName, String index, String type, Set<String> keySet) throws Exception;
}
