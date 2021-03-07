package org.eto.fom.context.annotation;

import java.util.Collection;

import org.eto.fom.context.core.Task;

/**
 * 
 * @author shanhm
 *
 */
public interface SchedulFactory<E> {

	/**
	 * 创建任务
	 * @return
	 * @throws Exception
	 */
	Collection<? extends Task<E>> newSchedulTasks() throws Exception;
}
