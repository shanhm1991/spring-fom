package com.fom.context.helper;

import java.util.List;

import org.apache.log4j.Logger;

import com.fom.log.LoggerFactory;

/**
 * 
 * @author shanhm
 *
 * @param <V> 行数据解析结果类型
 */
public abstract class AbstractParserHelper<V> implements ParserHelper<V> {
	
	protected Logger log = Logger.getRootLogger();
	
	public AbstractParserHelper(){
		
	}
	
	public AbstractParserHelper(String name){
		log = LoggerFactory.getLogger(name);
	}
	
	@Override
	public final void batchProcessLineData(List<V> lineDatas, long batchTime) throws Exception {
		if(Thread.interrupted()){
			throw new InterruptedException("interrupted when batchProcessLineData");
		}
		int size = lineDatas.size();
		batchProcessIfNotInterrupted(lineDatas, batchTime);
		log.info("批处理结束[" + size + "],耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
	}
	
	public abstract void batchProcessIfNotInterrupted(List<V> lineDatas, long batchTime) throws Exception;
	
}
