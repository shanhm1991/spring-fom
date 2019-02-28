package com.fom.pool.handler;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author shanhm
 *
 */
public interface JdbcHandler {
	
	JdbcHandler handler = new JdbcHandlerImpl();
	
	/**
	 * 查询
	 * @param poolName 连接池名称
	 * @param sql sql语句，其中参数变量以#修饰开头和结尾 
	 * @param paramMap 对应sql语句中的参数变量的值
	 * @return List
	 * @throws Exception Exception
	 */
	List<Map<String, Object>> queryForList(String poolName, String sql, Map<String, Object> paramMap) throws Exception;
	
	/**
	 * 新增/删除/修改
	 * @param poolName 连接池名称
	 * @param sql sql语句，其中参数变量以#修饰开头和结尾 
	 * @param paramMap 对应sql语句中的参数变量的值
	 * @return rows affects
	 * @throws Exception Exception
	 */
	int execute(String poolName, String sql,Map<String, Object> paramMap) throws Exception;

	/**
	 * 批量 新增/删除/修改
	 * @param poolName poolName 连接池名称
	 * @param sql sql语句，其中参数变量以#修饰开头和结尾 
	 * @param paramMaps 对应sql语句中的参数变量的值
	 * @return rows affects
	 * @throws Exception Exception
	 */
	int[] batchExecute(String poolName, String sql, List<Map<String, Object>> paramMaps) throws Exception;
	
	/**
	 * 开始事务
	 * @param poolName poolName
	 * @throws Exception Exception
	 */
	void startTransaction(String poolName) throws Exception;
	
	/**
	 * 结束事务
	 * @param poolName poolName
	 * @throws Exception Exception
	 */
	void endTransaction(String poolName) throws Exception;
}
