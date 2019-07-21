package org.eto.fom.util.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.dom4j.Element;
import org.eto.fom.util.xml.XmlUtil;

/**
 * 
 * @author shanhm
 *
 */
class JdbcPool extends Pool<Connection>{

	private static final int CORE_DEFAULT = 4;

	private static final int CORE_MIN = 1;

	private static final int CORE_MAX = 50;

	private static final int TIME_DEFAULT = 30000;

	private static final int TIME_MIN = 3000;

	private static final int TIME_MAX = 300000;

	private String url;

	private String user;

	private String passwd;

	private String driver;

	JdbcPool(String name){
		super(name);
	}

	@Override
	protected void load(Element el) throws Exception {
		int core = XmlUtil.getInt(el, "core", CORE_DEFAULT, CORE_MIN, CORE_MAX);
		int max = XmlUtil.getInt(el, "max", CORE_DEFAULT, CORE_MIN, CORE_MAX);
		int overTime = XmlUtil.getInt(el, "aliveTimeOut", TIME_DEFAULT, TIME_MIN, TIME_MAX);
		int waitTime = XmlUtil.getInt(el, "waitTimeOut", TIME_DEFAULT, TIME_MIN, TIME_MAX);
		String _driver = XmlUtil.getString(el, "driver", "");
		String _url = XmlUtil.getString(el, "url", "");
		String _user = XmlUtil.getString(el, "user", "");
		String _passwd = XmlUtil.getString(el, "passwd", "");
		if(this.core != core || this.max != max || this.aliveTimeOut != overTime
				|| !_driver.equals(this.driver) || !_url.equals(this.url) || !_user.equals(this.user) 
				|| !_passwd.equals(this.passwd) || this.waitTimeOut != waitTime){ 
			this.core = core;
			this.max = max;
			this.aliveTimeOut = overTime;
			this.waitTimeOut = waitTime;
			if(hasReset(_url, _user, _passwd, _driver)){
				synchronized (this) {
					this.url = _url;
					this.user = _user;
					this.passwd = _passwd; 
					this.driver = _driver;
					Class.forName(_driver, true, JdbcPool.class.getClassLoader());
				}
				acquire();
				release();
			}
			LOG.info("pool[" + name + "] created" + this);
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
		
		private static final int COST = 100;

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
				LOG.error("connection close failed, [pool=" + name + "]", e); 
			}
		}

		@Override
		public boolean isValid() {
			if(v == null){
				return false;
			}
			try{
				if(!v.isClosed() && v.isValid(COST)){
					return true;
				}
			}catch(SQLException e){
				LOG.error("connection check failed, [pool=" + name + "]", e); 
			}

			return false;
		}
	}
}
