package org.springframework.fom.support;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@Aspect
public class FomAdvice {

	private static Logger logger = LoggerFactory.getLogger(FomAdvice.class);

	private final IdGenerator idGenerator = new IdGenerator();

	private ThreadLocal<Long> localTime = new ThreadLocal<>();

	private ThreadLocal<String> localid = new ThreadLocal<>();

	@Pointcut("execution(* org.springframework.fom.*..*Controller.*(..)) && !execution(* org.springframework.fom.*..*Controller.handle*(..))")
	public void point() {

	}

	@Before("point()")
	public void logRequest(JoinPoint point) throws JsonProcessingException {
		localTime.set(System.currentTimeMillis());

		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = attributes.getRequest();
		String url = request.getRequestURI();
		String remote = request.getRemoteAddr();

		String requestId = request.getHeader("requestId");
		if(StringUtils.isBlank(requestId)){ 
			requestId = idGenerator.generateIdWithDate(null, "-", "yyyyMMddHHmmss", 1000);
		} 

		localid.set(requestId); 
		Thread.currentThread().setName(requestId); 

		Object[] args = point.getArgs();
		MethodSignature signature = (MethodSignature)point.getSignature();
		String[] params = signature.getParameterNames();
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			if(args[i] == null){
				map.put(params[i], null);
			}else{
				Class<?> clazz = args[i].getClass();
				if ("requestId".equals(params[i]) 
						|| HttpServletRequest.class.isAssignableFrom(clazz) 
						|| HttpServletResponse.class.isAssignableFrom(clazz)
						|| BeanPropertyBindingResult.class.equals(clazz)){
					continue;
				}
				map.put(params[i], new ObjectMapper().writeValueAsString(args[i])); 
			}
		}
		logger.info(">> request  {} {} {}", remote, url, map);
	}



	@AfterReturning(returning = "resp", pointcut = "point()")
	public Response<?> logResponse(Response<?> resp) { 
		resp.setRequestId(localid.get()); 
		logger.info("<< response {}ms {}", System.currentTimeMillis() - localTime.get(), resp);
		return resp;
	}
}
