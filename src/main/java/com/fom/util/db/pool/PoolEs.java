package com.fom.util.db.pool;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.fom.util.XmlUtil;
import com.fom.util.exception.WarnException;

/**
 * TransportClient本身提供了连接与通道的复用
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
class PoolEs extends Pool<TransportClient>{

	private String clusterName;

	private List<String> nodes;

	private volatile TransportClient client;
	
	private volatile EsNode clientNode = new EsNode();

	PoolEs(String name){
		super(name);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void load(Element el) throws Exception {
		String name = XmlUtil.getString(el, "clusterName", "");
		
		Element nodesE = el.element("nodes");
		Iterator<Element> it = nodesE.elementIterator("node");
		List<String> list = new ArrayList<>();
		while(it.hasNext()){
			list.add(it.next().getTextTrim());
		}

		if(hasReset(name, list)){
			this.clusterName = name;
			this.nodes = list;
			List<InetSocketTransportAddress> addressList = new ArrayList<>();
			for(String node : nodes){
				String[] array = node.split(":");
				String ip = array[0].trim();
				int port = Integer.parseInt(array[1].trim());
				addressList.add(new InetSocketTransportAddress(InetAddress.getByName(ip), port));
			}
			Settings settings = Settings.settingsBuilder()
			.put("cluster.name", clusterName)
			.put("client.transport.sniff", true)
			.build();
			
			TransportClient newClient = TransportClient.builder().settings(settings).build();
			for(InetSocketTransportAddress address : addressList){
				newClient.addTransportAddress(address);
			}
			
			if(client != null){
				client.close();
			}
			client = newClient;
			clientNode.v = client;
			LOG.info("#加载完成, " + name + this);
		}
	}

	private boolean hasReset(String name, List<String> list){
		return !name.equals(clusterName) || !list.equals(nodes);
	}
	
	@Override
	public EsNode acquire(){
		return clientNode;
	}
	
	@Override
	public void release() {
		//判断client是否可用，如果不可用则关闭，但是api中未找到相关方法
	}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("\n" + name + ".clusterName=" + clusterName);
		builder.append("\n" + name + ".nodes=" + nodes);
		return builder.toString();
	}

	@Override
	protected EsNode create() throws Exception {
		return null;
	}
	
	public static TransportClient getClient(String poolName) throws WarnException{
		PoolEs pool = (PoolEs)PoolManager.getPool(poolName);
		if(pool == null){
			throw new WarnException(poolName + "连接池不存在"); 
		}
		return pool.clientNode.v;
	}
	
	public class EsNode extends Node<TransportClient> {
		@Override
		void close() {
			
		}

		@Override
		boolean isValid() {
			return false;
		}

		@Override
		boolean isReset() {
			return false;
		}
		
	}
}
