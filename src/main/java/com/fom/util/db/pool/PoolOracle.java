package com.fom.util.db.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.dom4j.Element;

import com.fom.util.XmlUtil;

/**
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
class PoolOracle extends Pool<Connection>{

	static{
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (Exception e) {
			LOG.error("加载失败：oracle.jdbc.driver.OracleDriver", e); 
			throw new RuntimeException("加载失败：oracle.jdbc.driver.OracleDriver", e);
		}
	}

	private String oraUrl;

	private String oraUser;

	private String oraPasswd;

	PoolOracle(String name){
		super(name);
	}

	@Override
	protected void load(Element el) throws Exception {
		int core = XmlUtil.getInt(el, "core", 4, 1, 50);
		int max = XmlUtil.getInt(el, "max", 4, 1, 50);
		int overTime = XmlUtil.getInt(el, "aliveTimeOut", 15000, 3000, 60000);
		int waitTime = XmlUtil.getInt(el, "waitTimeOut", 15000, 3000, 60000);
		String oraUrl = XmlUtil.getString(el, "url", "");
		String oraUser = XmlUtil.getString(el, "user", "");
		String oraPasswd = XmlUtil.getString(el, "passwd", "");
		if(this.core != core || this.max != max || this.aliveTimeOut != overTime
				|| !oraUrl.equals(this.oraUrl) || !oraUser.equals(this.oraUser) 
				|| !oraPasswd.equals(this.oraPasswd) || this.waitTimeOut != waitTime){ 
			this.core = core;
			this.max = max;
			this.aliveTimeOut = overTime;
			this.waitTimeOut = waitTime;
			if(hasReset(oraUrl, oraUser, oraPasswd)){
				synchronized (this) {
					this.oraUrl = oraUrl;
					this.oraUser = oraUser;
					this.oraPasswd = oraPasswd; 
				}
				acquire();
				release();
			}
			LOG.info("#加载完成, " + name + this);
		}
	}
	
	private boolean hasReset(String url, String user, String passwd){
		return !url.equals(oraUrl) || !user.equals(oraUser) || !passwd.equals(oraPasswd);
	}

	@Override
	protected OracleNode create() throws Exception {
		return new OracleNode();
	}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("\n" + name + ".core=" + core);
		builder.append("\n" + name + ".max=" + max);
		builder.append("\n" + name + ".aliveTimeOut=" + aliveTimeOut);
		builder.append("\n" + name + ".waitTimeOut=" + waitTimeOut);
		builder.append("\n" + name + ".url=" + oraUrl);
		builder.append("\n" + name + ".user=" + oraUser);
		builder.append("\n" + name + ".passwd=" + oraPasswd);
		return builder.toString();
	}
	
	public class OracleNode extends Node<Connection> {

		private String url;

		private String user;

		private String passwd;
		
		public volatile boolean isInTransaction = false;

		public OracleNode() throws Exception{ 
			synchronized(PoolOracle.this){
				url = oraUrl;
				user = oraUser;
				passwd = oraPasswd;
			}
			v = DriverManager.getConnection(url,user,passwd); 
		}

		@Override
		public boolean isReset() {
			synchronized(PoolOracle.this){
				return hasReset(url, user, passwd);
			}
		}

		@Override
		public void close() {
			try {
				v.close();
			} catch (SQLException e) {
				LOG.error("连接关闭异常[" + name + "]", e); 
			}
		}

		@Override
		public boolean isValid() {
			if(v == null){
				return false;
			}
			try{
				if(!v.isClosed()){
					return true;
				}
			}catch(SQLException e){
				LOG.error("连接检查异常[" + name + "]", e); 
			}
			
			/**
			 * 有可能connnection对象没有关闭，但是它的连接已经失效，这会导致入库线程异常失败，
			 * 但是整体设计时考虑了失败恢复机制，所以可以忽略影响
			 */
			return false;
		}
	}
}
