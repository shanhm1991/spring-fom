package com.fom.db.handler;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.bulk.BulkItemResponse;

import com.fom.db.pool.EsHelper;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class EsHandlerImpl extends EsHelper implements EsHandler {

	@Override
	public boolean synCreateIndex(String poolName, String index, String type, File jsonFile) throws Exception { 
		synchronized (index.intern()) {
			if(_isIndexExist(poolName, index)){
				return false;
			}
			_createIndex(poolName, index);
			_mappingIndex(poolName, index, type, jsonFile);
			return true;
		}
	}

	public void synDelIndex(String poolName, String index) throws Exception { 
		synchronized (index.intern()) {
			if(!_isIndexExist(poolName, index)){
				return;
			}
			_delIndex(poolName, index);
		}
	}

	@Override
	public List<Map<String, Object>> multiGet(String poolName, String index, String type, Set<String> keySet) throws Exception {
		return _multiGet(poolName, index, type, keySet); 
	}

	@Override
	public Set<BulkItemResponse> bulkUpdate(String poolName, String index, String type, Map<String, Map<String, Object>> data) throws Exception {
		if(data == null || data.isEmpty()){
			return new HashSet<BulkItemResponse>();
		}
		return _bulkUpdate(poolName, index, type, data);
	}

	@Override
	public void bulkInsert(String poolName, String index, String type, Map<String, Map<String, Object>> data) throws Exception {
		if(data == null || data.isEmpty()){
			return;
		}
		_bulkInsert(poolName, index, type, data);
	}

	@Override
	public void blukDelete(String poolName, String index, String type, Set<String> keySet) throws Exception {
		if(keySet == null || keySet.isEmpty()){
			return;
		}
		_blukDelete(poolName, index, type, keySet); 
	}
}
