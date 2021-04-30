package org.springframework.fom.boot.test.executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.fom.ScheduleContext;

@Configuration
public class ExecConfiguration {
	
	
	// 直接在目标类标识@FomSchedule也一样的效果，只要把定时计划拿掉就可以当成线程池使用了，同样可以注入
	@Bean
	public ScheduleContext<Long> testExecutor(){
		return new ScheduleContext<>(true);
	}
}
