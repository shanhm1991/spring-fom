package com.fom.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

/**
 * 
 * @author shanhm
 * 
 */
public class HttpUtil {

	private static CloseableHttpClient httpClient;

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
						if (executionCount >= 2) {// 如果已经重试了2次，就放弃                    
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
	 * @param request request
	 * @param handler handler
	 * @return T
	 * @throws Exception Exception
	 */
	public static <T> T request(HttpUriRequest request, ResponseHandler<? extends T> handler) 
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
	 * @param request request
	 * @return CloseableHttpResponse
	 * @throws Exception Exception
	 */
	public static CloseableHttpResponse request(HttpUriRequest request) throws Exception{
		return httpClient.execute(request);
	}

	/**
	 * 下载文件
	 * @param url url
	 * @param file file
	 * @throws Exception Exception
	 */
	public static void download(String url, File file) throws Exception {
		CloseableHttpResponse resp = request(new HttpGet(url));
		InputStream input = null;
		FileOutputStream output = null;
		try{
			input = resp.getEntity().getContent();
			output = new FileOutputStream(file);
			int index;
			byte[] bytes = new byte[1024];
			while ((index = input.read(bytes)) != -1) {
				output.write(bytes, 0, index);
				output.flush();
			}
		}finally{
			IoUtil.close(input);
			IoUtil.close(output);
		}
	}

	/**
	 * 获取文件流
	 * @param url url
	 * @return InputStream
	 * @throws Exception Exception
	 */
	public static InputStream open(String url) throws Exception {
		return request(new HttpGet(url)).getEntity().getContent();
	}

	/**
	 * 删除文件
	 * @param url url
	 * @return int 返回码
	 * @throws Exception Exception
	 */
	public static int delete(String url) throws Exception {
		CloseableHttpResponse response= request(new HttpDelete(url));
		return response.getStatusLine().getStatusCode();
	}

	/**
	 * 上传文件
	 * @param url url
	 * @param params 参数列表
	 * @param localFile 文件列表
	 * @return 返回码
	 * @throws Exception Exception
	 */
	public static int upload(File localFile, String url, Map<String,String> params) throws Exception{    
		MultipartEntityBuilder mEntityBuilder = MultipartEntityBuilder.create();  
		mEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);  
		mEntityBuilder.setCharset(Charset.forName("utf-8"));  
		// 普通参数  
		ContentType contentType = ContentType.create("text/plain",Charset.forName("utf-8"));
		if (params != null && !params.isEmpty()) {  
			Set<String> keySet = params.keySet();  
			for (String key : keySet) {  
				mEntityBuilder.addTextBody(key, params.get(key),contentType);  
			}  
		}  
		//二进制参数  
		mEntityBuilder.addBinaryBody("file", localFile);  
		
		HttpPost httpost = new HttpPost(url); 
		httpost.setEntity(mEntityBuilder.build());  
		CloseableHttpResponse response = request(httpost);
		return response.getStatusLine().getStatusCode();
	}   
}
