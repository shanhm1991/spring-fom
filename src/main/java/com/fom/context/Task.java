package com.fom.context;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 任务
 * <br>Callable的抽象实现，在context中作为最小执行单位，也可以单独创建调用或者提交线程池
 * 
 * @author shanhm
 *
 */
public abstract class Task implements Callable<Result> {

	protected volatile Logger log = Logger.getRootLogger();

	/**
	 * 任务唯一标识
	 */
	protected final String id;
	
	/**
	 * 异常处理器
	 */
	protected ExceptionHandler exceptionHandler;

	/**
	 * 结果处理器
	 */
	protected ResultHandler resultHandler;
	
	private volatile Context context;
	
	/**
	 * @param id 创建Executor的资源
	 */
	public Task(String id) { 
		this.id = id;
	}

	/**
	 * @param id 唯一标识
	 * @param exceptionHandler 异常处理器
	 */
	public Task(String id, ExceptionHandler exceptionHandler) { 
		this(id);
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * @param id 唯一标识
	 * @param resultHandler 结果处理器
	 */
	public Task(String id, ResultHandler resultHandler) { 
		this(id);
		this.resultHandler = resultHandler;
	}

	/**
	 * @param id 唯一标识
	 * @param exceptionHandler 异常处理器
	 * @param resultHandler 结果处理器
	 */
	public Task(String id, ExceptionHandler exceptionHandler, ResultHandler resultHandler) { 
		this(id);
		this.exceptionHandler = exceptionHandler;
		this.resultHandler = resultHandler;
	}

	@Override
	public final Result call() {  
		Thread.currentThread().setName(id);
		Result result = new Result(id); 
		long sTime = System.currentTimeMillis();
		result.startTime = sTime;
		
		try {
			result.success = beforeExec() && exec() && afterExec();
		} catch(Throwable e) {
			result.success = false;
			result.throwable = e;
			log.error(e); 
			if(exceptionHandler != null){
				exceptionHandler.handle(e); 
			}
		}
		
		result.costTime = System.currentTimeMillis() - sTime;
		if(resultHandler != null){
			try{
				resultHandler.handle(result);
			}catch(Exception e){
				result.success = false;
				log.error(e);
				if(result.throwable == null){
					result.throwable = e;
				}
			}
		}
		
		//这里算上resulthandler的结果和耗时
		long cost = System.currentTimeMillis() - sTime;
		if(result.success){
			context.statistics.successIncrease(id, cost); 
			log.info("task success, cost=" + cost + "ms");
		}else{
			context.statistics.failedIncrease(id, result.throwable);
			log.warn("task failed, cost=" + cost + "ms");
		}
		return result;
	}

	/**
	 * 任务执行
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected abstract boolean exec() throws Exception;

	/**
	 * 任务执行前的工作
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected boolean beforeExec() throws Exception {
		return true;
	}
	
	/**
	 * 任务执行后的工作
	 * @return isSuccess
	 * @throws Exception Exception
	 */
	protected boolean afterExec() throws Exception {
		return true;
	}
	
	final void setContext(Context context){
		if(context == null){
			return;
		}
		this.context = context;
		this.log = LoggerFactory.getLogger(context.name); 
	}
	
	/**
	 * 只有在context中使用时才会赋值，否则将为null，
	 */
	protected final String getContextName(){
		if(context == null){
			return null;
		}
		return context.getName();
	}
	
	/**
	 * 获取context配置值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return 配置值
	 */
	protected final String getContextValue(String key, String defaultValue){
		if(context == null){
			return defaultValue;
		}
		return context.getString(key, defaultValue);
	}
	
	/**
	 * 获取context配置int值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return 配置值
	 */
	protected final int getContextIntValue(String key, int defaultValue){
		if(context == null){
			return defaultValue;
		}
		return context.getInt(key, defaultValue);
	}
	
	/**
	 * 获取context配置long值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return 配置值
	 */
	protected final long getContextLongValue(String key, long defaultValue){
		if(context == null){
			return defaultValue;
		}
		return context.getLong(key, defaultValue);
	}

	/**
	 * 获取context配置boolean值
	 * @param key key
	 * @param defaultValue defaultValue
	 * @return 配置值
	 */
	protected final boolean getContextBooleanValue(String key, boolean defaultValue){
		if(context == null){
			return defaultValue;
		}
		return context.getBoolean(key, defaultValue);
	}
}
