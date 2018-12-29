package com.fom.util.db.handler;

import java.util.List;
import java.util.Map;

import com.fom.util.db.pool.SqlHelper;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class SqlHandlerImpl extends SqlHelper implements SqlHandler {
	
	@Override
	public List<Map<String, Object>> queryForList(String poolName, String sql, Map<String, Object> paramMap) throws Exception {
		return _queryForList(poolName, sql, paramMap);
	}

	@Override
	public int execute(String poolName, String sql, Map<String, Object> paramMap) throws Exception {
		return _execute(poolName, sql, paramMap); 
	}

	@Override
	public int[] batchExecute(String poolName, String sql, List<Map<String, Object>> paramMaps) throws Exception {
		return _batchExecute(poolName, sql, paramMaps);
	}
	
	public void startTransaction(String poolName) throws Exception {
		_startTransaction(poolName); 
	}
	
	public void endTransaction(String poolName) throws Exception {
		_endTransaction(poolName); 
	}
}
