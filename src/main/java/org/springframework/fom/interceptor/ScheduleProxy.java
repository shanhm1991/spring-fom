package org.springframework.fom.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.fom.ScheduleContext;
import org.springframework.fom.Task;
import org.springframework.fom.annotation.FomSchedule;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class ScheduleProxy implements MethodInterceptor {

	private ScheduleContext<?> scheduleContext;
	
	private String beanName;

	private Object scheduleBean;

	private Class<?> scheduleBeanClass;

	public ScheduleProxy(String beanName, ScheduleContext<?> scheduleContext, FomSchedule fomSchedule, Object scheduleBean){
		this.scheduleContext = scheduleContext;
		this.beanName = beanName;
		this.scheduleBean = scheduleBean;
		if(scheduleBean != null){
			this.scheduleBeanClass = scheduleBean.getClass();
		}
	}

	@Override
	public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
		String methodName = method.getName();
		if("onScheduleComplete".equals(methodName)){ 
			return onScheduleComplete(object, method, args, methodProxy);
		}else if("onScheduleTerminate".equals(methodName)){
			return onScheduleTerminate(object, method, args, methodProxy);
		}else if("newSchedulTasks".equals(methodName)){
			return newSchedulTasks(object, method, args, methodProxy);
		}else{
			return methodProxy.invoke(scheduleContext, args);
		}
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object onScheduleComplete(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable{
		Class<?>[] parameterTypes = method.getParameterTypes();
		if(scheduleBean == null 
				|| !(ScheduleCompleter.class.isAssignableFrom(scheduleBeanClass))
				|| parameterTypes.length != 3){
			return methodProxy.invokeSuper(object, args);
		}

		if(long.class.isAssignableFrom(parameterTypes[0])
				&& long.class.isAssignableFrom(parameterTypes[1])
				&& List.class.isAssignableFrom(parameterTypes[2])){
			ScheduleCompleter scheduleCompleter = (ScheduleCompleter)scheduleBean;
			scheduleCompleter.onScheduleComplete((long)args[0], (long)args[1], (List)args[2]); 
			return null;
		}else{
			return methodProxy.invokeSuper(object, args);
		}
	}

	private Object onScheduleTerminate(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable{
		Class<?>[] parameterTypes = method.getParameterTypes();
		if(scheduleBean == null 
				|| !(ScheduleTerminator.class.isAssignableFrom(scheduleBeanClass))
				|| parameterTypes.length != 2){
			return methodProxy.invokeSuper(object, args);
		}

		if(long.class.isAssignableFrom(parameterTypes[0])
				&& long.class.isAssignableFrom(parameterTypes[1])){
			ScheduleTerminator scheduleTerminator = (ScheduleTerminator)scheduleBean;
			scheduleTerminator.onScheduleTerminate((long)args[0], (long)args[1]);
			return null;
		}else{
			return methodProxy.invokeSuper(object, args);
		}
	}

	// TODO 这里对于添加了@Scheduled的method，还应该对本身的方法做个过滤，比如在newSchedulTasks上添加@Scheduled则忽略
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object newSchedulTasks(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable{
		Class<?>[] parameterTypes = method.getParameterTypes();
		if(parameterTypes.length != 0){
			return methodProxy.invokeSuper(object, args);
		}
		
		Collection<Task<?>> tasks = new ArrayList<>();
		if(scheduleBean == null){ // 已经是继承ScheduleContext实现的
			tasks.addAll((Collection<Task<?>>)methodProxy.invokeSuper(object, args)); 
			List<Method> methods = new ArrayList<>();
			for(Method m : scheduleContext.getClass().getMethods()){
				Scheduled scheduled = m.getAnnotation(Scheduled.class);
				if(scheduled != null){
					methods.add(m);
				}
			}
			
			int index = 0;
			for(final Method m : methods){
				Task<Object> task = new Task<Object>(beanName + "-task-" + ++index){
					@Override
					public Object exec() throws Exception {
						return m.invoke(scheduleContext);
					}
				}; 
				tasks.add(task);
			}
		}else{ // 普通类上面添加了FomSchedule
			if((ScheduleFactory.class.isAssignableFrom(scheduleBeanClass))){
				ScheduleFactory scheduleFactory = (ScheduleFactory)scheduleBean;
				Collection<Task<?>> collection =  (Collection<Task<?>>)scheduleFactory.newSchedulTasks();
				if(!CollectionUtils.isEmpty(collection)){
					tasks.addAll(collection);
				}
			}

			List<Method> methods = new ArrayList<>();
			for(Method m : scheduleBeanClass.getMethods()){
				Scheduled scheduled = m.getAnnotation(Scheduled.class);
				if(scheduled != null){
					methods.add(m);
				}
			}

			int index = 0;
			for(final Method m : methods){
				Task<Object> task = new Task<Object>(beanName + "-task-" + ++index){
					@Override
					public Object exec() throws Exception {
						return m.invoke(scheduleBean);
					}
				}; 
				tasks.add(task);
			}
		}
		return tasks;
	}
	
	
}