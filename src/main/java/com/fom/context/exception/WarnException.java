package com.fom.context.exception;

/**
 * 
 * @author shanhm
 *
 */
public class WarnException extends RuntimeException {
	private static final long serialVersionUID = -7951679678790524526L;

	public WarnException(String message) {
		super(message); 
	}
	
	public WarnException(String message, Exception e) {
		super(message, e); 
	}
}
