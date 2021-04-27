package org.springframework.fom.support.controller;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.fom.ScheduleInfo;
import org.springframework.fom.support.FomAdvice;
import org.springframework.fom.support.Response;
import org.springframework.fom.support.Response.Page;
import org.springframework.fom.support.service.FomService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
@RequestMapping("/fom")
public class FomController {
	
	private static Logger logger = LoggerFactory.getLogger(FomController.class);

	@Autowired
	private FomService fomService;

	@RequestMapping("/schedule/list")
	@ResponseBody
	public Response<Page<ScheduleInfo>> list() {
		List<ScheduleInfo> list = fomService.list();
		return new Response<>(Response.SUCCESS, "", new Page<>(list, list.size()));
	}
	
	@RequestMapping("/schedule/info")
	@ResponseBody
	public Response<ScheduleInfo> info(String scheduleName) {
		return new Response<>(Response.SUCCESS, "", fomService.info(scheduleName)); 
	}
	
	@RequestMapping("/schedule/logger/level")
	@ResponseBody
	public Response<String> loggerLevel(String scheduleName) {
		return new Response<>(Response.SUCCESS, "", fomService.getLoggerLevel(scheduleName)); 
	}
	
	@RequestMapping("/schedule/logger/level/update")
	@ResponseBody
	public Response<Void> updateloggerLevel(String scheduleName, String levelName) {
		fomService.updateloggerLevel(scheduleName, levelName);
		return new Response<>(Response.SUCCESS, ""); 
	}
	
	@RequestMapping("/schedule/start")
	@ResponseBody
	public Response<Void> start(String scheduleName) {
		return fomService.start(scheduleName);
	}
	
	@RequestMapping("/schedule/shutdown")
	@ResponseBody
	public Response<Void> shutdown(String scheduleName) {
		return fomService.shutdown(scheduleName);
	}
	
	@RequestMapping("/schedule/exec")
	@ResponseBody
	public Response<Void> exec(String scheduleName) {
		return fomService.exec(scheduleName);
	}
	
	@RequestMapping("/schedule/waitings")
	@ResponseBody
	public Response<Map<String, String>> waitingTasks(String scheduleName) {
		return new Response<>(Response.SUCCESS, "", fomService.getWaitingTasks(scheduleName));  
	}
	
	@RequestMapping("/schedule/actives")
	@ResponseBody
	public Response<List<Map<String, String>>> activeTasks(String scheduleName) {
		return new Response<>(Response.SUCCESS, "", fomService.getActiveTasks(scheduleName));  
	}
	
	@RequestMapping("/schedule/success")
	@ResponseBody
	public Response<Map<String, Object>> successStat(String scheduleName, String endDay) throws ParseException { 
		return new Response<>(Response.SUCCESS, "", fomService.getSuccessStat(scheduleName, endDay)); 
	}
	
	/*************************Handlers************************************/
	@Autowired
	private FomAdvice fomAdvice;
	
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseBody
	public Response<Void> handle(HttpRequestMethodNotSupportedException e){
		logger.error("", e); 
		Response<Void> resp = new Response<>(Response.ILLEGAL, "request not support");
		fomAdvice.logResponse(resp);
		return resp;
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseBody
	public Response<Void> handle(MethodArgumentNotValidException e){
		logger.error("", e); 
		BindingResult binding = e.getBindingResult();
        if (binding.hasErrors()) {
            List<ObjectError> errors = binding.getAllErrors();
            if (!errors.isEmpty()) {
                FieldError fieldError = (FieldError)errors.get(0);
                Response<Void> resp = new Response<>(Response.ILLEGAL, fieldError.getDefaultMessage());
        		fomAdvice.logResponse(resp);
        		return resp;
            }
        }
        
        Response<Void> resp = new Response<>(Response.ILLEGAL, "illegal request");
		fomAdvice.logResponse(resp);
		return resp;
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseBody
	public Response<Void> handle(IllegalArgumentException e){
		logger.error("", e); 
		Response<Void> resp = new Response<>(Response.ILLEGAL, e.getMessage());
		fomAdvice.logResponse(resp);
		return resp;
	}
	
	@ExceptionHandler(ValidationException.class)
	@ResponseBody
	public Response<Void> handle(ValidationException e){
		logger.error("", e);
		Response<Void> resp = new Response<>(Response.ILLEGAL, e.getMessage());
		fomAdvice.logResponse(resp);
		return resp;
	}
	
	@ExceptionHandler(Exception.class)
	@ResponseBody
	public Response<Void> handle(Exception e){
		logger.error("", e); 
		Response<Void> resp = new Response<>(Response.ERROR, e.getMessage());
		fomAdvice.logResponse(resp);
		return resp;
	}
}
