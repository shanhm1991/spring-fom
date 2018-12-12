package com.fom.util.db.pool;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.dom4j.Element;

/**
 * 
 * @author X4584
 * @date 2018年8月10日
 */
class PoolHttp extends Pool<HttpURLConnection> {

	private String url;

	PoolHttp(String name){
		super(name);
	}

	@Override
	protected void load(Element el) throws Exception {
		int core = getInt(el, "core", 4, 1, 8);
		int max = getInt(el, "max", 4, 1, 8);
		int overTime = getInt(el, "aliveTime", 30 * 1000, 3 * 1000, 300 * 1000);
		String url = getString(el, "url", "");
		if(this.core != core || this.max != max || this.overTime != overTime || !url.equals(this.url)){
			this.core = core;
			this.max = max;
			this.overTime = overTime;
			if(!url.equals(this.url)){
				synchronized (this) {
					this.url = url;
				}
				acquire();
				release();
			}
			LOG.info("#### 加载完成, " + name + this);
		}
	}

	@Override
	protected HttpNode create() throws Exception {
		return new HttpNode();
	}
	
	public class HttpNode extends Node<HttpURLConnection> {

		private String esUrl;

		public HttpNode() throws Exception{ 
			synchronized(PoolHttp.this){
				esUrl = url;
			}
			URL _url = new URL(esUrl);
			v = (HttpURLConnection) _url.openConnection();
			v.setRequestProperty("Accept-Charset", "UTF-8");
			v.setRequestProperty("connection", "keep-Alive");
			v.setRequestProperty("Content-Type", "application/json");
			v.setRequestProperty("Accept","application/json");
			v.setConnectTimeout(3000);
			v.connect();
		}

		@Override
		boolean isReset() {
			synchronized(PoolHttp.this){
				return !esUrl.equals(url);
			}
		}

		@Override
		void close() {
			v.disconnect();
		}

		@Override
		boolean isValid() {
			try {
				int status = v.getResponseCode();
				if(status == 200){
					return true;
				}
			} catch (IOException e) {

			}
			return false;
		}
	}
}
