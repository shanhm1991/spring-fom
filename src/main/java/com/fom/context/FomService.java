package com.fom.context;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * fom提供运维页面接口
 * 
 * @author shanhm
 *
 */
public interface FomService {

	/**
	 * 获取容器中所有的context模块
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String, Object> list() throws Exception;

	/**
	 * 更新context的配置项
	 * @param name context名称
	 * @param data json数据
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String, Object> save(String name, String data) throws Exception;

	/**
	 * 启动
	 * @param name context名称
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String,Object> startup(String name) throws Exception;

	/**
	 * 停止
	 * @param name context名称
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String,Object> shutDown(String name) throws Exception;

	/**
	 * 立即运行
	 * @param name context名称
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String,Object> execNow(String name) throws Exception;

	/**
	 * 获取context状态
	 * @param name context名称
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String,Object> state(String name) throws Exception;

	/**
	 * 新建context模块
	 * @param json json数据
	 * @return map结果
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
	 * 获取成功的任务耗时详情
	 * @param name context名称
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String,Object> successDetail(String name) throws Exception;

	/**
	 * 获取context正在执行的任务线程的堆栈
	 * @param name context名称
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String,Object> activeDetail(String name) throws Exception;
	
	/**
	 * 获取失败的任务详情
	 * @param name context名称
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String,Object> failedDetail(String name) throws Exception;

	/**
	 * 获取正在等待的任务详情
	 * @param name context名称
	 * @return map结果
	 * @throws Exception Exception
	 */
	Map<String,Object> waitingdetail(String name) throws Exception;
	
	/**
	 * 获取其他log的级别
	 * @return log名称与级别
	 * @throws Exception Exception
	 */
	Map<String, String> listOtherLogs() throws Exception;
	
	/**
	 * 查询日志级别
	 * @param logger logger
	 * @return level
	 */
	String queryLevel(String logger);
	
	/**
	 * 保存日志级别
	 * @param logger logger
	 * @param level level
	 */
	void saveLevel(String logger, String level);
	
	/**
	 * 保存耗时统计区间
	 * @param name name
	 * @param levelStr levelStr
	 * @param saveDay saveDay
	 * @param date date
	 * @return map
	 * @throws Exception Exception
	 */
	Map<String,Object> saveCostLevel(String name, String levelStr, String saveDay, String date) throws Exception;
	
	/**
	 * 调整起止日期
	 * @param name name
	 * @param date date
	 * @return map
	 * @throws Exception Exception
	 */
	Map<String,Object> changeDate(String name, String date) throws Exception;

	/**
	 * 获取成功任务明细
	 * @param name name
	 * @param resp resp
	 * @throws Exception Exception
	 */
	void dataDownload(String name, HttpServletResponse resp) throws Exception;
	
}
