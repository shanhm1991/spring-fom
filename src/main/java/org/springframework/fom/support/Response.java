package org.springframework.fom.support;

import java.util.Collection;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class Response<T> {

	public static final int SUCCESS = 200;

	public static final int ILLEGAL = 400;

	public static final int ERROR = 500;

	public static final int FAILED = 501;

	private final int code;

	private final String msg;

	private String requestId;

	private T data;

	public Response(int code, String msg){
		this.code = code;
		this.msg = msg;
	}

	public Response(int code, String msg, T data){
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public int getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	@Override
	public String toString() {
		return "{requestId=" + requestId + ", code=" + code + ", msg=" + msg + ", data=" + data + "}";
	}

	public static class Page<V> {

		private int total;

		private Collection<V> list;

		public Page(){

		}

		public Page(Collection<V> list, int total){
			this.list = list;
			this.total = total;
		}

		public Page(Collection<V> list, long total){
			this.list = list;
			this.total = (int)total;
		}

		public int getTotal() {
			return total;
		}

		public void setTotalRows(int total) {
			this.total = total;
		}

		public Collection<V> getList() {
			return list;
		}

		public void setList(Collection<V> list) {
			this.list = list;
		}

		@Override
		public String toString() {
			return "{total=" + total + ", list=" + list + "}";
		}
	}
}
