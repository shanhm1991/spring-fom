package com.fom.db.pool;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;
import com.fom.util.IoUtil;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class JdbcHelper {

	protected static final Logger LOG = LoggerFactory.getLogger("jdbc");

	protected JdbcHelper(){

	}

	protected final void _startTransaction(String poolName) throws Exception {
		JdbcPool pool = getPool(poolName);
		JdbcPool.JdbcNode node = (JdbcPool.JdbcNode)pool.acquire();
		if(node.isInTransaction){
			LOG.warn("事务开启失败[Connection is already in Transaction]");
			return;
		}
		node.isInTransaction = true;
		Connection con = node.v;
		try{
			LOG.info("开启事务");
			con.commit();
			con.setAutoCommit(false); 
		}catch(Exception e){
			node.isInTransaction = false;
			con.setAutoCommit(true); 
			pool.release();
			LOG.error("开启事务异常", e);
		}
	}
	
	protected final void _endTransaction(String poolName) throws Exception {
		JdbcPool pool = getPool(poolName);
		JdbcPool.JdbcNode node = (JdbcPool.JdbcNode)pool.acquire();
		if(!node.isInTransaction){
			pool.release();
			LOG.warn("提交事务失败[Connection is not in Transaction]");
			return;
		}

		Connection con = node.v;
		try{
			con.commit();
			LOG.info("提交事务");
		}catch(Exception e){
			LOG.error("提交事务异常,数据回滚", e); 
			con.rollback();
		}finally{
			node.isInTransaction = false;
			con.setAutoCommit(true); 
			pool.release();
		}
	}
	
	private boolean isInTransaction(JdbcPool pool) throws Exception{ 
		JdbcPool.JdbcNode node = (JdbcPool.JdbcNode)pool.acquire();
		return node.isInTransaction;
	}
	
	/**
	 * 取消事务，直接回滚,释放连接
	 * @param poolName
	 * @throws Exception
	 */
	private void cancelTransaction(String poolName) throws Exception {
		if(!isInTransaction(getPool(poolName))){
			return;
		}

		JdbcPool pool = getPool(poolName);
		try{
			JdbcPool.JdbcNode node = (JdbcPool.JdbcNode)pool.acquire();
			if(!isInTransaction(getPool(poolName))){
				LOG.warn("取消事务失败[Connection is not in Transaction]");
				return;
			}
			Connection con = node.v;
			LOG.info("取消事务,数据回滚");
			con.rollback();
			con.setAutoCommit(true); 
		}finally{
			pool.release();
		}
	}

	protected final List<Map<String, Object>> _queryForList(String poolName, String sql, Map<String, Object> paramMap) throws Exception {
		StringBuilder sqlBuilder = new StringBuilder(sql);
		Map<String, Integer> indexMap = praseSQL(sqlBuilder);
		sql = sqlBuilder.toString();
		if(!isInTransaction(getPool(poolName))){
			return queryForList(poolName, sql, paramMap, indexMap);
		}else{
			return queryForListTransaction(poolName, sql, paramMap, indexMap);
		}
	}

	private List<Map<String, Object>> queryForList(String poolName, String sql, Map<String, Object> paramMap, Map<String, Integer> indexMap) throws Exception {
		JdbcPool pool = getPool(poolName);
		PreparedStatement ps = null;
		ResultSet resultSet = null;
		try{
			Connection con = pool.acquire().v;
			ps = con.prepareStatement(sql);
			setParams(con, ps, paramMap, indexMap);
			LOG.info("[query]=" + sql);
			LOG.info("[param]=" + logParam(ps, paramMap, indexMap)); 
			long eTime = System.currentTimeMillis();
			List<Map<String, Object>> list = praseResultSet(ps.executeQuery());
			LOG.info("[affects=" + list.size() + ",cost=" + (System.currentTimeMillis() - eTime) + "ms]");
			return list;
		}finally{
			pool.release();
			IoUtil.close(resultSet); 
			IoUtil.close(ps); 
		}
	}

	private List<Map<String, Object>> queryForListTransaction(String poolName, String sql, Map<String, Object> paramMap, Map<String, Integer> indexMap) throws Exception {
		JdbcPool pool = getPool(poolName);
		PreparedStatement ps = null;
		ResultSet resultSet = null;
		try{
			Connection con = pool.acquire().v;
			ps = con.prepareStatement(sql);
			setParams(con, ps, paramMap, indexMap);
			LOG.info("[query]=" + sql);
			LOG.info("[param]=" + logParam(ps, paramMap, indexMap)); 
			long eTime = System.currentTimeMillis();
			List<Map<String, Object>> list = praseResultSet(ps.executeQuery());
			LOG.info("[affects=" + list.size() + ",cost=" + (System.currentTimeMillis() - eTime) + "ms]");
			return list;
		}catch(Exception e){
			cancelTransaction(poolName);
			throw new Exception("回滚数据,事务执行异常", e);
		}finally{
			IoUtil.close(resultSet); 
			IoUtil.close(ps); 
		}
	}

	protected final int _execute(String poolName, String sql, Map<String, Object> paramMap) throws Exception {
		StringBuilder sqlBuilder = new StringBuilder(sql);
		Map<String, Integer> indexMap = praseSQL(sqlBuilder);
		sql = sqlBuilder.toString();
		if(isInTransaction(getPool(poolName))){
			return executeTransaction(poolName, sql, paramMap, indexMap);
		}else{
			return execute(poolName, sql, paramMap, indexMap);
		}
	}

	private int execute(String poolName, String sql, Map<String, Object> paramMap, Map<String, Integer> indexMap) throws Exception { 
		JdbcPool pool = getPool(poolName);
		PreparedStatement ps = null;
		try{
			Connection con = pool.acquire().v;
			ps = con.prepareStatement(sql);
			setParams(con, ps, paramMap, indexMap);
			LOG.info("[execute]=" + sql);
			LOG.info("[param]=" + logParam(ps, paramMap, indexMap)); 
			long eTime = System.currentTimeMillis();
			int num = ps.executeUpdate();
			LOG.info("[affects=" + num + ",cost=" + (System.currentTimeMillis() - eTime) + "ms]");
			return num;
		}finally{
			IoUtil.close(ps); 
			pool.release();
		}
	}

	private int executeTransaction(String poolName, String sql, Map<String, Object> paramMap, Map<String, Integer> indexMap) throws Exception { 
		PreparedStatement ps = null;
		try{
			Connection con = getPool(poolName).acquire().v;
			ps = con.prepareStatement(sql);
			setParams(con, ps, paramMap, indexMap);
			LOG.info("[execute in transaction]=" + sql);
			LOG.info("[param] = " + logParam(ps, paramMap, indexMap)); 
			long eTime = System.currentTimeMillis();
			int num = ps.executeUpdate();
			LOG.info("[affects=" + num + ",cost=" + (System.currentTimeMillis() - eTime) + "ms]");
			return num;
		}catch(Exception e) {
			cancelTransaction(poolName);
			throw new Exception("回滚数据,事务执行异常", e);
		}finally{
			IoUtil.close(ps); 
		}
	}

	protected final int[] _batchExecute(String poolName, String sql, List<Map<String, Object>> paramMaps) throws Exception {
		StringBuilder sqlBuilder = new StringBuilder(sql);
		Map<String, Integer> indexMap = praseSQL(sqlBuilder);
		sql = sqlBuilder.toString();
		if(isInTransaction(getPool(poolName))){
			return batchExecuteTransaction(poolName, sql, paramMaps, indexMap);
		}else{
			return batchExecute(poolName, sql, paramMaps, indexMap);
		}

	}

	private int[] batchExecute(String poolName, String sql, List<Map<String, Object>> paramMaps, Map<String, Integer> indexMap) throws Exception {
		JdbcPool pool = getPool(poolName);
		PreparedStatement ps = null;
		try{
			Connection con = pool.acquire().v;
			ps = con.prepareStatement(sql);
			for(Map<String, Object> map : paramMaps){
				setParams(con, ps, map, indexMap);
				ps.addBatch();
			}
			LOG.info("[batch execute]=" + sql);
			long eTime = System.currentTimeMillis();
			int[] nums = ps.executeBatch();
			int num = 0;
			for(int i : nums){
				num += i;
			}
			LOG.info("[affects=" + num + ",cost=" + (System.currentTimeMillis() - eTime) + "ms]");
			return nums;
		}finally{
			IoUtil.close(ps); 
			pool.release();
		}
	}

	private int[] batchExecuteTransaction(String poolName, String sql, List<Map<String, Object>> paramMaps, Map<String, Integer> indexMap) throws Exception {
		PreparedStatement ps = null;
		try{
			Connection con = getPool(poolName).acquire().v;
			ps = con.prepareStatement(sql);
			for(Map<String, Object> map : paramMaps){
				setParams(con, ps, map, indexMap);
				ps.addBatch();
			}
			LOG.info("[batch execute in transaction]=" + sql);
			long eTime = System.currentTimeMillis();
			int[] nums = ps.executeBatch();
			int num = 0;
			for(int i : nums){
				num += i;
			}
			LOG.info("[affects=" + num + ",cost=" + (System.currentTimeMillis() - eTime) + "ms]");
			return nums;
		}catch(Exception e){
			cancelTransaction(poolName);
			throw new Exception("事务执行异常", e);
		}finally{
			IoUtil.close(ps); 
		}
	}

	

	private JdbcPool getPool(String poolName) { 
		JdbcPool pool = (JdbcPool)PoolManager.get(poolName);
		if(pool == null){
			throw new RuntimeException(poolName + "连接池不存在");
		}
		return pool;
	}

	private Map<String,Integer> praseSQL(StringBuilder sqlBuilder){
		Map<String,Integer> indexMap = new HashMap<String,Integer>();
		String sql = sqlBuilder.toString();
		if(sql.indexOf("#") == -1){
			return indexMap;
		}

		int index = 0;
		while(sql.indexOf("#") != -1){
			int start = sql.indexOf("#");
			int end = sql.indexOf("#", start + 1);
			String key = sql.substring(start + 1, end);
			index++;
			indexMap.put(key, index);
			sql = sql.substring(0, start) + "?" + sql.substring(end + 1);
		}

		sqlBuilder.delete(0, sqlBuilder.length()).append(sql);
		return indexMap;
	}

	private void setParams(Connection con, PreparedStatement ps, Map<String, Object> paramMap, Map<String, Integer> indexMap) throws Exception{ 
		for(Entry<String, Integer> entry : indexMap.entrySet()){
			String key = entry.getKey();
			Integer index = entry.getValue();
			Object value = paramMap.get(key);
			if(value == null){
				throw new IllegalArgumentException("参数未赋值param[" + key + "]");
			}
			ps.setObject(index, value);
		}
	}

	private String logParam(PreparedStatement ps, Map<String, Object> paramMap, Map<String, Integer> indexMap) {
		StringBuilder builder = new StringBuilder("[");
		if(indexMap == null || indexMap.isEmpty()){
			return builder.append("]").toString();
		}else{
			int count = 0;
			for(Entry<String, Integer> entry : indexMap.entrySet()){
				String key = entry.getKey();
				builder.append(key);
				builder.append("=");
				String value = String.valueOf(paramMap.get(key));
				if(value.length() > 30){
					value = value.substring(0, 30) + "...";
				}
				builder.append(value);
				count++;
				if(count < indexMap.size()){
					builder.append(",");
				}
			}
		}
		//		暂时不用AbstractMethodError
		//		try {
		//			ParameterMetaData pdata = ps.getParameterMetaData();
		//			int count = pdata.getParameterCount();
		//			for(int i = 1; i <= count; i++){
		//				//String type = pdata.getParameterTypeName(i); 不支持的特性
		//				String value = getParamValue(i, paramMap, indexMap);
		//				if(i < count){
		//					builder.append(value).append(", ");
		//					continue;
		//				}
		//				builder.append(value);
		//			}
		//		} catch (SQLException e) {
		//			LOG.error(e); 
		//		}
		return builder.append("]").toString();
	}

	private List<Map<String, Object>> praseResultSet(ResultSet resultSet) throws SQLException{
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		while (resultSet.next()) {
			list.add(praseResult(resultSet));
		}
		return list;
	}

	private Map<String, Object> praseResult(ResultSet resultSet) throws SQLException {
		Map<String, Object> result = new HashMap<String, Object>();
		ResultSetMetaData rsmd = resultSet.getMetaData();

		int cols = rsmd.getColumnCount();
		for (int i = 1; i <= cols; ++i) {
			String colName = rsmd.getColumnLabel(i);
			if ((colName == null) || (colName.length() == 0)) {
				colName = rsmd.getColumnName(i);
			}

			Object colValue = resultSet.getObject(i);
			int colType = rsmd.getColumnType(i);
			if ((colValue == null) && (((1 == colType) || (12 == colType) || (-1 == colType)))) {
				colValue = "";
			} else if ((2005 == colType) || (Clob.class.isInstance(colValue))) {
				colValue = processClob(resultSet.getClob(i));
			} else {
				colValue = processBigDecimal(colValue, resultSet, i);
			}
			result.put(colName, colValue);
		}
		return result;
	}

	private Object processBigDecimal(Object obj, ResultSet rs, int columIndex) throws SQLException {
		if (obj instanceof BigInteger) {
			obj = Long.valueOf(((BigInteger) obj).longValue());
		} else if (obj instanceof BigDecimal) {
			BigDecimal bd = (BigDecimal) obj;
			if ((bd.scale() == 0) || (bd.toString().indexOf(46) == -1)) {
				obj = Long.valueOf(bd.longValue());
			} else
				obj = Float.valueOf(bd.floatValue());

		}
		return obj;
	}

	private String processClob(Clob clob) throws SQLException {
		if (clob == null) {
			return "";
		}

		BufferedReader reader = new BufferedReader(clob.getCharacterStream());
		StringBuilder sb = new StringBuilder("");
		if (reader != null) {
			try {
				char[] buffer = new char[1024];
				int c = -1;
				while ((c = reader.read(buffer)) != -1)
					sb.append(buffer, 0, c);
			} catch (IOException e) {
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
				}
				reader = null;
			}
		}
		return sb.toString();
	}

	@Deprecated
	@SuppressWarnings({ "unused", "null" })
	private void batchProduce(long[] idArray) throws Exception { 
		long start = System.currentTimeMillis();

		Connection con = null; //getConnect();

		StructDescriptor structDesc = StructDescriptor.createDescriptor("TAG_WXUSER_TEMP", con);

		long sqlTime = start / 1000;
		List<STRUCT> structList = new ArrayList<STRUCT>();
		for (int i = 0;i < idArray.length;i++) {
			Object[] record = {idArray[i],idArray[i],sqlTime,sqlTime,0,1,0,0,0,0};
			STRUCT struct = new STRUCT(structDesc, con, record);
			structList.add(struct);
		}
		ArrayDescriptor arrayDesc = ArrayDescriptor.createDescriptor("TAG_WXUSER_LIST", con); 
		ARRAY array = new ARRAY(arrayDesc, con, structList.toArray()); 

		CallableStatement cstmt = (CallableStatement) con.prepareCall("{call MERGE_WXUSER(?)}");
		cstmt.setArray(1, array); 
		cstmt.execute();
		con.commit();
		cstmt.close();
	}
}
