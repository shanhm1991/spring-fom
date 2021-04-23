package org.springframework.fom.support.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.fom.ScheduleInfo;
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

	@RequestMapping("/list")
	@ResponseBody
	public Response<Page<ScheduleInfo>> list() {
		List<ScheduleInfo> list = fomService.list();
		return new Response<>(Response.SUCCESS, "", new Page<>(list, list.size()));
	}
	
	@RequestMapping("/info")
	@ResponseBody
	public Response<ScheduleInfo> list(String scheduleName) {
		return new Response<>(Response.SUCCESS, "", fomService.info(scheduleName)); 
	}
	
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseBody
	public Response<Void> handle(HttpServletRequest request, HttpRequestMethodNotSupportedException e){
		logger.error("", e); 
		return new Response<>(Response.ILLEGAL, "request not support");
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseBody
	public Response<Void> handle(HttpServletRequest request, MethodArgumentNotValidException e){
		logger.error("", e); 
		BindingResult binding = e.getBindingResult();
        if (binding.hasErrors()) {
            List<ObjectError> errors = binding.getAllErrors();
            if (!errors.isEmpty()) {
                FieldError fieldError = (FieldError)errors.get(0);
                return new Response<>(Response.ILLEGAL, fieldError.getDefaultMessage());
            }
        }
		return new Response<>(Response.ILLEGAL, "illegal request");
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseBody
	public Response<Void> handle(HttpServletRequest request, IllegalArgumentException e){
		logger.error("", e); 
		return new Response<>(Response.ILLEGAL, e.getMessage());
	}
	
	@ExceptionHandler(ValidationException.class)
	@ResponseBody
	public Response<Void> handle(HttpServletRequest request, ValidationException e){
		logger.error("", e);
		return new Response<>(Response.ILLEGAL, e.getMessage());
	}
	
	@ExceptionHandler(Exception.class)
	@ResponseBody
	public Response<Void> handle(HttpServletRequest request, Exception e){
		logger.error("", e); 
		return new Response<>(Response.ERROR, e.getMessage());
	}
}
