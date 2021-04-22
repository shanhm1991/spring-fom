package org.springframework.fom.spring;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.fom.ScheduleConfig;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.Task;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = {SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class SpringTest implements ApplicationContextAware{

	ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}

	@SuppressWarnings({"unchecked", "rawtypes" })
	@Test 
	public void test1() throws Exception{
		TestContextBean testContext = context.getBean(TestContextBean.class);
		Logger logger = testContext.getLogger();

		logger.info("TestContextBean：测试属性"); 
		Assert.assertEquals(testContext.getScheduleName(), "testContextBean");
		Assert.assertEquals(testContext.getScheduleBeanName(), null);
		
		ScheduleConfig scheduleConfig = testContext.getScheduleConfig();
		logger.info("TestContextBean：测试配置"); 
		Assert.assertEquals(scheduleConfig.getCronExpression(), "0/15 * * * * ?");
		Assert.assertEquals(scheduleConfig.getRemark(), "备注测试");

		logger.info("TestContextBean：测试配置中的spring环境变量");
		Assert.assertEquals(scheduleConfig.getString("conf.user", ""), "shanhm1991");
		Assert.assertEquals(scheduleConfig.getString("conf.address", ""), "163.com");

		logger.info("TestContextBean：测试方法代理调用");
		testContext.onScheduleComplete(100, 100, null);
		testContext.onScheduleTerminate(200, 200);
		Collection<Task<Long>> tasks1 = (Collection<Task<Long>>)testContext.newSchedulTasks();
		Assert.assertEquals(tasks1.size(), 2);
		for(Task task : tasks1){
			Assert.assertEquals(20L, task.exec());
		}

	}

	@SuppressWarnings("unchecked")
	@Test 
	public void test2() throws Exception{
		ScheduleContext<Long> scheduleContext = (ScheduleContext<Long>)context.getBean("$testScheduleBean");
		Logger logger = scheduleContext.getLogger();

		logger.info("TestScheduleBean：测试属性"); 
		Assert.assertEquals(scheduleContext.getScheduleName(), "testScheduleBean");
		Assert.assertEquals(scheduleContext.getScheduleBeanName(), "testScheduleBean");

		logger.info("TestScheduleBean：测试配置中的spring环境变量");
		ScheduleConfig scheduleConfig = scheduleContext.getScheduleConfig();
		Assert.assertEquals(scheduleConfig.getString("conf.user", ""), "shanhm1991");
		Assert.assertEquals(scheduleConfig.getString("conf.address", ""), "163.com");

		logger.info("TestScheduleBean：测试方法代理调用");
		scheduleContext.onScheduleComplete(100, 100, null);
		scheduleContext.onScheduleTerminate(200, 200);
		Collection<Task<Long>> tasks2 = (Collection<Task<Long>>)scheduleContext.newSchedulTasks();
		Assert.assertEquals(tasks2.size(), 2);
		for(Task<Long> task : tasks2){
			long result = task.exec();
			Assert.assertEquals(10, result);
		}

	}
}
