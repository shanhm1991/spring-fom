package com.fom.pool.handler;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.bulk.BulkItemResponse;

/**
 * 
 * @author shanhm
 *
 */
public interface EsHandler {
	
	EsHandler handler = new EsHandlerImpl();

	/**
	 * 同步判断新建索引
	 * @param poolName poolName
	 * @param index index
	 * @param type type
	 * @param jsonFile jsonFile
	 * @return 创建结果
	 * @throws Exception Exception
	 */
	boolean synCreateIndex(String poolName, String index, String type, File jsonFile) throws Exception;

	/**
	 * 同步删除索引
	 * @param poolName poolName
	 * @param index index
	 * @throws Exception Exception
	 */
	void synDelIndex(String poolName, String index) throws Exception;

	/**
	 * 批量查询,根据主键
	 * @param poolName poolName
	 * @param index index
	 * @param type type
	 * @param keySet keySet
	 * @return List
	 * @throws Exception Exception
	 */
	List<Map<String,Object>> multiGet(String poolName, String index, String type, Set<String> keySet) throws Exception;

	/**
	 * 批量更新
	 * @param poolName poolName
	 * @param index index
	 * @param type type
	 * @param data Map
	 * @return 由于VersionConflictEngineException而失败的数据
	 * @throws Exception Exception
	 */
	Set<BulkItemResponse> bulkUpdate(String poolName, String index, String type, Map<String,Map<String,Object>> data) throws Exception;

	/**
	 * 批量新增
	 * @param poolName poolName
	 * @param index index
	 * @param type type
	 * @param data Map
	 * @throws Exception Exception
	 */
	void bulkInsert(String poolName, String index, String type, Map<String,Map<String,Object>> data) throws Exception;

	/**
	 * 批量删除
	 * @param poolName poolName
	 * @param index index
	 * @param type type
	 * @param keySet keySet
	 * @throws Exception Exception
	 */
	void blukDelete(String poolName, String index, String type, Set<String> keySet) throws Exception;
}
