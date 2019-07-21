package org.eto.fom.context;

/**
 * Task结果处理器
 * 
 * @param <E> 结果数据类型
 * 
 * @see Task
 * 
 * @author shanhm
 *
 */
public interface ResultHandler<E> {

	void handle(Result<E> result) throws Exception;
}
