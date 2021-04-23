package org.springframework.fom.support;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class Response<T> {

	public static final int SUCCESS = 200;
	
	public static final int FAILED = 501;

	public static final int ERROR = 500;

	public static final int ILLEGAL = 400;

	private final int code;

	private final String msg;
	
	private String requestId;
	
	private T result;
	
	public Response(int code, String msg){
		this.code = code;
		this.msg = msg;
	}
	
	public Response(int code, String msg, T result){
		this.code = code;
		this.msg = msg;
		this.result = result;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}

	public int getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	@Override
	public String toString() {
		return "{requestId=" + requestId + ", code=" + code + ", msg=" + msg + ", result=" + result + "}";
	}
	
	
}
