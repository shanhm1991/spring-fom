package org.springframework.fom;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class TaskDelayed implements Delayed {
	
	@SuppressWarnings("rawtypes")
	private final TimedFuture future;
	
    private final long delayTime;
    
    @SuppressWarnings("rawtypes")
	public TaskDelayed(TimedFuture future, long delaySeconds) {
    	this.future = future;
        this.delayTime = System.currentTimeMillis() + delaySeconds;
    }
    
    @Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(delayTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public int compareTo(Delayed delayTask) {
		TaskDelayed d = (TaskDelayed) delayTask;
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - d.getDelay(TimeUnit.MILLISECONDS));
	}

	@SuppressWarnings("rawtypes")
	public TimedFuture getFuture() {
		return future;
	}
}
