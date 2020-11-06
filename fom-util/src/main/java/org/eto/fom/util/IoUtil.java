package org.eto.fom.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.net.SocketClient;
import org.apache.log4j.Logger;

/**
 * 
 * @author shanhm
 *
 */
public class IoUtil {
	
	private static final Logger LOG = Logger.getLogger(IoUtil.class);
	
	public static void close(AutoCloseable con){
		if(con == null){
			return;
		}
		try{
			con.close();
		}catch(Exception e){
			LOG.error("", e);
		}
	}
	
	public static void close(SocketClient client){
		if(client == null){
			return;
		}
		try{
			client.disconnect();
		}catch(Exception e){
			LOG.error("", e);
		}
	}
	
	public static <E> int intersection(List<? extends E> src, List<? super E> dest, int limit) {
		if(CollectionUtils.isEmpty(src) || dest == null){
			return 0;
		}
		
		int count = 0;
		Iterator<? extends E> it = src.iterator();
		while(it.hasNext() && ++count <= limit){
			E e = it.next();
			it.remove();
			dest.add(e);
			
		}
		return count;
	}
	
	
	public static void main(String[] args) {
		List<Integer> list1 = new ArrayList<>();
		list1.add(1);
		list1.add(2);
		list1.add(3);
		list1.add(4);
		list1.add(5);
		list1.add(6);
		list1.add(7);
		list1.add(8);
		list1.add(9);
		list1.add(10);
		list1.add(11);
		list1.add(12);
		
		List<Integer> list2 = new ArrayList<>();
		
		while(intersection(list1, list2, 5) > 0){
			System.out.println(list2);
			list2.clear();
		}
		
		
		
	}
}
