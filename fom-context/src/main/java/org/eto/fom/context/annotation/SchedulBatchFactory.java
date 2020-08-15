package org.eto.fom.context.annotation;

import java.util.Collection;

import org.eto.fom.context.core.Task;

/**
 * 
 * @author shanhm
 *
 */
public interface SchedulBatchFactory<E> {

	Collection<? extends Task<E>> creatTasks() throws Exception;
}
