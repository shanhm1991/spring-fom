package org.springframework.fom.support.controller;

import java.util.List;

import javax.validation.ValidationException;

import org.springframework.fom.support.Response;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 
 * 没有直接使用@ControllerAdvice，避免影响应用中实际的业务Controller
 * 
 * @author shanhm1991@163.com
 *
 */
public class FomExceptionController {

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseBody
	public Response<Void> handle(HttpRequestMethodNotSupportedException e){
		return new Response<>(Response.ILLEGAL, "request not support");
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseBody
	public Response<Void> handle(MethodArgumentNotValidException e){
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
	public Response<Void> handle(IllegalArgumentException e){
		return new Response<>(Response.ILLEGAL, e.getMessage());
	}
	
	@ExceptionHandler(ValidationException.class)
	@ResponseBody
	public Response<Void> handle(ValidationException e){
		return new Response<>(Response.ILLEGAL, e.getMessage());
	}
	
	@ExceptionHandler(Exception.class)
	@ResponseBody
	public Response<Void> handle(Exception e){
		return new Response<>(Response.ERROR, e.getMessage());
	}
}
