package com.fom.db.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.dom4j.Element;

import com.fom.util.XmlUtil;

/**
 * 
 * @author shanhm
 *
 */
class JdbcPool extends Pool<Connection>{

	private String url;

	private String user;

	private String passwd;
	
	private String driver;

	JdbcPool(String name){
		super(name);
	}

	@Override
	protected void load(Element el) throws Exception {
		int core = XmlUtil.getInt(el, "core", 4, 1, 50);
		int max = XmlUtil.getInt(el, "max", 4, 1, 50);
		int overTime = XmlUtil.getInt(el, "aliveTimeOut", 30000, 3000, 300000);
		int waitTime = XmlUtil.getInt(el, "waitTimeOut", 30000, 3000, 300000);
		String driver = XmlUtil.getString(el, "driver", "");
		String url = XmlUtil.getString(el, "url", "");
		String user = XmlUtil.getString(el, "user", "");
		String passwd = XmlUtil.getString(el, "passwd", "");
		if(this.core != core || this.max != max || this.aliveTimeOut != overTime
				|| !driver.equals(this.driver) || !url.equals(this.url) || !user.equals(this.user) 
				|| !passwd.equals(this.passwd) || this.waitTimeOut != waitTime){ 
			this.core = core;
			this.max = max;
			this.aliveTimeOut = overTime;
			this.waitTimeOut = waitTime;
			if(hasReset(url, user, passwd, driver)){
				synchronized (this) {
					this.url = url;
					this.user = user;
					this.passwd = passwd; 
					this.driver = driver;
					Class.forName(driver, true, JdbcPool.class.getClassLoader());
				}
				acquire();
				release();
			}
			LOG.info("init pool[" + name + "] " + this);
		}
	}
	
	private boolean hasReset(String url, String user, String passwd, String driver){
		return !url.equals(this.url) || !user.equals(this.user)  
				|| !passwd.equals(this.passwd) || !driver.equals(this.driver); 
	}

	@Override
	protected JdbcNode create() throws Exception {
		return new JdbcNode();
	}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("\n" + name + ".core=" + core);
		builder.append("\n" + name + ".max=" + max);
		builder.append("\n" + name + ".aliveTimeOut=" + aliveTimeOut);
		builder.append("\n" + name + ".waitTimeOut=" + waitTimeOut);
		builder.append("\n" + name + ".driver=" + driver);
		builder.append("\n" + name + ".url=" + url);
		builder.append("\n" + name + ".user=" + user);
		builder.append("\n" + name + ".passwd=" + passwd);
		return builder.toString();
	}
	
	public class JdbcNode extends Node<Connection> {

		private String nodeUrl;

		private String nodeUser;

		private String nodePasswd;
		
		public volatile boolean isInTransaction = false;

		public JdbcNode() throws Exception{ 
			synchronized(JdbcPool.this){
				nodeUrl = url;
				nodeUser = user;
				nodePasswd = passwd;
			}
			v = DriverManager.getConnection(nodeUrl,nodeUser,nodePasswd); 
		}

		@Override
		public boolean isReset() {
			synchronized(JdbcPool.this){
				return hasReset(nodeUrl, nodeUser, nodePasswd, driver);
			}
		}

		@Override
		public void close() {
			try {
				v.close();
			} catch (SQLException e) {
				LOG.error("close failed[" + name + "]", e); 
			}
		}

		@Override
		public boolean isValid() {
			if(v == null){
				return false;
			}
			try{
				if(!v.isClosed() && v.isValid(100)){
					return true;
				}
			}catch(SQLException e){
				LOG.error("check failed[" + name + "]", e); 
			}
			
			return false;
		}
	}
}
