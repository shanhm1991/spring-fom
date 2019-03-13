package com.fom.context;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;

import org.apache.hadoop.util.ThreadUtil;
import org.apache.log4j.Logger;

class Monitor {

	private static final Logger LOG = Logger.getLogger(Monitor.class);

	public static void start(){

		new Thread("fom-monitor"){
			@Override
			public void run(){
				while(true){
					ThreadUtil.sleepAtLeastIgnoreInterrupts(10 * 60000);
					listen();
				}
			}
		}.start();
	}

	private static void listen(){
		StringBuilder builder = new StringBuilder("monitor:\n");
		String pid = ManagementFactory.getRuntimeMXBean().getName();
		pid = pid.split("@")[0];
		builder.append("pid=").append(pid);

		MemoryMXBean memorymbean = ManagementFactory.getMemoryMXBean();   
		MemoryUsage usage = memorymbean.getHeapMemoryUsage(); 
		builder.append("\nheap-init=").append(usage.getInit() / 1024 / 1024).append("MB");
		builder.append("\nheap-max=").append(usage.getMax() / 1024 / 1024).append("MB");
		builder.append("\nheap-used=").append(usage.getUsed() / 1024 / 1024).append("MB");
		builder.append("\nmemory-total=").append(Runtime.getRuntime().totalMemory() / 1024 / 1024).append("MB");
		
		MemoryUsage nonusage = memorymbean.getNonHeapMemoryUsage(); 
		builder.append("\nmemory-NonHeap=").append(nonusage.getUsed() / 1024 / 1024).append("MB");

		List<MemoryPoolMXBean> mpmList=ManagementFactory.getMemoryPoolMXBeans();  
		for(MemoryPoolMXBean mpm:mpmList){  
			String name = mpm.getName();
			MemoryUsage musage = mpm.getUsage();
			builder.append("\nmemory[").append(name).append("]: init=").append(getKB(musage.getInit()))
			.append("KB, used=").append(getKB(musage.getUsed())).append("KB, committed=")
			.append(getKB(musage.getCommitted())).append("KB, max=").append(getKB(musage.getMax())).append("KB");
		}  

		ThreadMXBean tm=(ThreadMXBean)ManagementFactory.getThreadMXBean();  
		builder.append("\nThread[total]: ").append(tm.getThreadCount());
		builder.append("\nThread[daemon]: ").append(tm.getDaemonThreadCount());

		List<GarbageCollectorMXBean> gcList = ManagementFactory.getGarbageCollectorMXBeans();  
		for(GarbageCollectorMXBean gc : gcList){  
			builder.append("\nGC[").append(gc.getName())
			.append("]: count=").append(gc.getCollectionCount()).append(", cost=").append(gc.getCollectionTime()).append("ms");
		}  
		LOG.info(builder.toString()); 
	}

	private static long getKB(long len){
		if(len <= 0){
			return 0;
		}
		return len / 1024;
	}



}
