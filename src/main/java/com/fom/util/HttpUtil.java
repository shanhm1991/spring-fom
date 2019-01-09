package com.fom.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HttpUtil {

	private static final CloseableHttpClient httpClient;

	static{
		ConnectionSocketFactory csf = PlainConnectionSocketFactory.getSocketFactory();
		LayeredConnectionSocketFactory lcsf = SSLConnectionSocketFactory.getSocketFactory();
		Registry<ConnectionSocketFactory> registry = 
				RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", csf)
				.register("https", lcsf)
				.build();
		PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(registry);
		manager.setMaxTotal(32768); 
		manager.setDefaultMaxPerRoute(500);
		httpClient = HttpClients.custom()
				.setConnectionManager(manager)
				.setRetryHandler(new HttpRequestRetryHandler() {
					public boolean retryRequest(IOException exception,int executionCount, HttpContext context) {
						if (executionCount >= 3) {// 如果已经重试了3次，就放弃                    
							return false;
						}
						if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试                    
							return true;
						}
						if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常                    
							return false;
						}                
						if (exception instanceof InterruptedIOException) {// 超时                    
							return false;
						}
						if (exception instanceof UnknownHostException) {// 目标服务器不可达                    
							return false;
						}
						if (exception instanceof ConnectTimeoutException) {// 连接被拒绝                    
							return false;
						}
						if (exception instanceof SSLException) {// ssl握手异常                    
							return false;
						}

						HttpClientContext clientContext = HttpClientContext.adapt(context);
						HttpRequest request = clientContext.getRequest();
						if (!(request instanceof HttpEntityEnclosingRequest)) {                    
							return true;
						}
						return false;
					}
				}).build();
	}

	/**
	 * 建议传入回调handler,避免遗忘关闭response
	 * @param request
	 * @param handler
	 * @return
	 * @throws Exception
	 */
	public static final <T> T request(HttpUriRequest request, ResponseHandler<? extends T> handler) 
			throws Exception{
		CloseableHttpResponse response = null;
		try{
			response = httpClient.execute(request);
			return handler.handleResponse(response);
		}finally{
			IoUtil.close(response);
		}
	}
	
	/**
	 * 需要自行关闭response
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public static final CloseableHttpResponse request(HttpUriRequest request) throws Exception{
		return httpClient.execute(request);
	}
}