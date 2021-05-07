package org.springframework.fom.boot.test.executor;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.fom.Result;
import org.springframework.fom.ScheduleConfig;
import org.springframework.fom.ScheduleContext;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Configuration
public class ExecConfiguration {
	
	// 直接在目标类标识@FomSchedule也一样的效果，只要把定时计划拿掉就可以当成线程池使用了，同样可以注入
	@Bean
	public ScheduleContext<Long> testExecutor(){
		ScheduleContext<Long> scheduleContext = new ScheduleContext<Long>(){
			@Override
			public void onScheduleComplete(long execTimes, long lastExecTime, List<Result<Long>> results) throws Exception {
				logger.info("第{}次在{}提交的任务全部结束，结果为{}", execTimes, lastExecTime, results); 
			}
			
			@Override
			protected void record(Result<Long> result) {
				super.record(result);
				// 自定义任务结果统计
			}
		};
		
		// 自定义配置
		ScheduleConfig scheduleConfig = scheduleContext.getScheduleConfig();
		scheduleConfig.setThreadCore(10);
		scheduleConfig.reset();
		return scheduleContext;
		
		// 如果直接使用默认配置，可以简单如下
		// return new ScheduleContext<>(true);
	}
}
