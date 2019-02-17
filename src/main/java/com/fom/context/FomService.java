package com.fom.context;

import java.util.Map;

/**
 * fom提供运维页面接口
 * 
 * @author shanhm
 *
 */
public interface FomService {

	/**
	 * 获取容器中所有的context模块
	 * @return Map<String, Object>
	 * @throws Exception Exception
	 */
	Map<String, Object> list() throws Exception;
	
	/**
	 * 更新context的配置项
	 * @param name context名称
	 * @param data json数据
	 * @return Map<String, Object>
	 * @throws Exception Exception
	 */
	Map<String, Object> save(String name, String data) throws Exception;
	
	/**
	 * 启动
	 * @param name context名称
	 * @return Map<String,Object>
	 * @throws Exception Exception
	 */
	Map<String,Object> startup(String name) throws Exception;
	
	/**
	 * 停止
	 * @param name context名称
	 * @return Map<String,Object>
	 * @throws Exception Exception
	 */
	Map<String,Object> shutDown(String name) throws Exception;
	
	/**
	 * 立即运行
	 * @param name context名称
	 * @return Map<String,Object>
	 * @throws Exception Exception
	 */
	Map<String,Object> execNow(String name) throws Exception;
	
	/**
	 * 获取context状态
	 * @param name context名称
	 * @return Map<String,Object>
	 * @throws Exception Exception
	 */
	Map<String,Object> state(String name) throws Exception;
	
	/**
	 * 新建context模块
	 * @param json json数据
	 * @return Map<String,Object>
	 * @throws Exception Exception
	 */
	Map<String,Object> create(String json)  throws Exception;
	
	/**
	 * 修改context日志级别
	 * @param name context名称
	 * @param level 日志级别
	 */
	void changeLogLevel(String name, String level);
	
	/**
	 * 获取context正在执行的任务线程的堆栈
	 * @param name context名称
	 * @return Map<String,Object>
	 * @throws Exception Exception
	 */
	Map<String,Object> getActiveThreads(String name) throws Exception;
	
}
