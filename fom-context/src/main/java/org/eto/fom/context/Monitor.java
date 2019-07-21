package org.eto.fom.context;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.List;

import org.apache.hadoop.util.ThreadUtil;
import org.apache.log4j.Logger;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.Swap;

public class Monitor {

	private static final Logger LOG = Logger.getLogger(Monitor.class);

	private final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

	private final MemoryMXBean memorymbean = ManagementFactory.getMemoryMXBean();   

	private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();  

	private final OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();

	private final DecimalFormat format = new DecimalFormat("#.##%");

	public static final Monitor INSTANCE = new Monitor();
	
	private static final long UNIT = 1024;
	
	private static final long DELAY = 1000;
	
	private static final double PERCENT = 100D;
	
	private static final int DISK = 2;
	
	public void monitorJvm(){
		String tName = Thread.currentThread().getName();
		Thread monitor = new Thread("jvm-monitor-" + tName){
			@Override
			public void run(){
				try {
					jvm();
				} catch (InterruptedException e) {
					return;
				}
			}
		};
		monitor.setDaemon(true); 
		monitor.start();
	}
	
	public void jvm() throws InterruptedException{ 
		long preTime = System.nanoTime();
		long preUsedTime = 0;
		for (long id : threadBean.getAllThreadIds()) {
			ThreadInfo info = threadBean.getThreadInfo(id);
			if(info == null){
				continue;
			}
			preUsedTime += threadBean.getThreadCpuTime(id);
		}

		Thread.sleep(DELAY);
		StringBuilder builder = new StringBuilder("======= jvm status: ");
		String pid = runtime.getName().split("@")[0];
		builder.append("pid=").append(pid);

		MemoryUsage usage = memorymbean.getHeapMemoryUsage(); 
		builder.append("\nmemory[heap-init]MB: ").append(usage.getInit() / UNIT / UNIT);
		builder.append("\nmemory[heap-max]MB: ").append(usage.getMax() / UNIT / UNIT);
		builder.append("\nmemory[heap-used]MB: ").append(usage.getUsed() / UNIT / UNIT);
		MemoryUsage nonusage = memorymbean.getNonHeapMemoryUsage(); 
		builder.append("\nmemory[Non-Heap]MB: ").append(nonusage.getUsed() / UNIT / UNIT);

		List<MemoryPoolMXBean> mpmList=ManagementFactory.getMemoryPoolMXBeans();  
		for(MemoryPoolMXBean mpm:mpmList){  
			String name = mpm.getName();
			MemoryUsage musage = mpm.getUsage();
			builder.append("\nmemory[").append(name).append("]KB: init=").append(getKB(musage.getInit()))
			.append(", used=").append(getKB(musage.getUsed())).append(", committed=")
			.append(getKB(musage.getCommitted())).append(", max=").append(getKB(musage.getMax()));
		}  

		List<GarbageCollectorMXBean> gcList = ManagementFactory.getGarbageCollectorMXBeans();  
		for(GarbageCollectorMXBean gc : gcList){  
			builder.append("\nGC[").append(gc.getName())
			.append("]: count=").append(gc.getCollectionCount()).append(", cost=").append(gc.getCollectionTime()).append("ms");
		}  

		builder.append("\nThread[total]: ").append(threadBean.getThreadCount());
		builder.append("\nThread[daemon]: ").append(threadBean.getDaemonThreadCount());
		long usedTime = 0;
		for (long id : threadBean.getAllThreadIds()) {
			ThreadInfo info = threadBean.getThreadInfo(id);
			if(info == null){
				continue;
			}
			String name = info.getThreadName();
			String state = info.getThreadState().name();
			long cpuTime = threadBean.getThreadCpuTime(id);
			usedTime += cpuTime;
			long userTime = threadBean.getThreadUserTime(id);
			builder.append("\nThread[id=").append(id).append("]: name=").append(name)
			.append(", state=").append(state).append(", cpuTime=").append(cpuTime).append(", userTime=").append(userTime);
		}

		long currTime = System.nanoTime();
		long passedTime = currTime - preTime;
		long currUsedTime = usedTime - preUsedTime;

		int processors = osMxBean.getAvailableProcessors();
		builder.append("\nprocessors[available]: ").append(processors);

		double rate = ((double) currUsedTime) / passedTime / processors;
		builder.append("\nCPU[rate]: " + format.format(rate));
		LOG.info(builder.toString()); 
	}
	
	

	private long getKB(long len){
		if(len <= 0){
			return 0;
		}
		return len / UNIT;
	}
	
	public void monitorOps(long delay){
		ThreadUtil.sleepAtLeastIgnoreInterrupts(delay);
		monitorOps();
	}

	public void monitorOps(){
		String tName = Thread.currentThread().getName();
		Thread monitor = new Thread("opsMonitor-" + tName){
			@Override
			public void run(){
				Sigar sigar = new Sigar();
				StringBuilder builder = new StringBuilder("======= ops status: ");
				disk(sigar, builder);
				memory(sigar, builder);
				cpu(sigar, builder);
				network(sigar, builder);
				LOG.info(builder.toString()); 
			}
		};
		monitor.setDaemon(true); 
		monitor.start();
	}
	
	private boolean disk(Sigar sigar, StringBuilder builder) {
		builder.append("\n======= os disk:");
		try{
			for (FileSystem fs : sigar.getFileSystemList()) {
				if(fs.getType() != DISK){ //本地硬盘
					continue;
				}
				builder.append("\n盘符名称: ").append(fs.getDevName());
				builder.append("\n盘符路径: ").append(fs.getDirName()); 
				builder.append("\n盘符标志: ").append(fs.getFlags()); 
				builder.append("\n盘符类型: ").append(fs.getSysTypeName()); 
				FileSystemUsage usage = sigar.getFileSystemUsage(fs.getDirName());
				builder.append("\n总量MB: ").append(usage.getTotal() / UNIT);
				builder.append("\n剩余MB: ").append(usage.getFree() / UNIT);
				builder.append("\n可用MB: ").append(usage.getAvail() / UNIT);
				builder.append("\n已经使用MB: ").append(usage.getUsed() / UNIT);
				builder.append("\n读出MB：").append(usage.getDiskReadBytes() / UNIT / UNIT);
				builder.append("\n写入MB：").append(usage.getDiskWriteBytes() / UNIT / UNIT);
				builder.append("\n资源的利用率: ").append(usage.getUsePercent() * PERCENT).append("%");
			}
		}catch(Throwable e){
			LOG.error("", e);
			return false;
		}
		return true;
	}

	private boolean memory(Sigar sigar, StringBuilder builder) {
		builder.append("\n======= os memory:");
		try{
			Mem mem = sigar.getMem();
			builder.append("\n内存总量MB: ").append(mem.getTotal() / UNIT /UNIT);
			builder.append("\n内存使用量MB: ").append(mem.getUsed() / UNIT /UNIT);
			builder.append("\n内存剩余量MB: ").append(mem.getFree() / UNIT /UNIT);
			Swap swap = sigar.getSwap();
			builder.append("\n交换区总量MB: ").append(swap.getTotal() / UNIT /UNIT);
			builder.append("\n交换区使用量MB: ").append(swap.getUsed() / UNIT /UNIT);
			builder.append("\n交换区剩余量MB: ").append(swap.getFree() / UNIT /UNIT);
		}catch(Throwable e){
			LOG.error("", e);
			return false;
		}
		return true;
	}

	private boolean cpu(Sigar sigar, StringBuilder builder) {
		builder.append("\n======= os cpu:");
		try{
			CpuInfo infos[] = sigar.getCpuInfoList();
			CpuPerc cpuList[] = sigar.getCpuPercList();
			for (int i = 0; i < infos.length; i++) {
				CpuInfo info = infos[i];
				CpuPerc perc = cpuList[i];
				builder.append("\ncpu_" + (i + 1));
				builder.append("\ncpu总量MHz: ").append(info.getMhz());
				builder.append("\ncpu缓存数量: ").append(info.getCacheSize());
				builder.append("\ncpu总的使用率: ").append(CpuPerc.format(perc.getCombined()));
				builder.append("\ncpu用户使用率: ").append(CpuPerc.format(perc.getUser()));
				builder.append("\ncpu系统使用率: ").append(CpuPerc.format(perc.getSys()));
				builder.append("\ncpu当前等待率: ").append(CpuPerc.format(perc.getWait()));
				builder.append("\ncpu当前错误率: ").append(CpuPerc.format(perc.getNice()));
				builder.append("\ncpu当前空闲率: ").append(CpuPerc.format(perc.getIdle()));
			}
		}catch(Throwable e){
			LOG.error("", e);
			return false;
		}
		return true;
	}

	private boolean network(Sigar sigar, StringBuilder builder) {
		builder.append("\n======= os network:");
		try{
			for (String name : sigar.getNetInterfaceList()) {
				NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
				builder.append("\n网卡设备名: ").append(name); 
				builder.append("\n描述: ").append(ifconfig.getDescription());
				builder.append("\nIp: ").append(ifconfig.getAddress());
				builder.append("\nMac: ").append(ifconfig.getHwaddr());
				builder.append("\n子网掩码: ").append(ifconfig.getNetmask());
				if ((ifconfig.getFlags() & 1L) <= 0L) {
					builder.append("\n!IFF_UP...skipping getNetInterfaceStat");
					continue;
				}
				NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);
				builder.append("\n接收的总包裹数: ").append(ifstat.getRxPackets());
				builder.append("\n发送的总包裹数: ").append(ifstat.getTxPackets());
				builder.append("\n接收的总字节数KB: ").append(ifstat.getRxBytes() / UNIT);
				builder.append("\n发送的总字节数KB: ").append(ifstat.getTxBytes() / UNIT);
				builder.append("\n接收的错误包裹数: ").append(ifstat.getRxErrors());
				builder.append("\n发送的错误包裹数: ").append(ifstat.getTxErrors());
				builder.append("\n接收时丢弃的包裹数: ").append(ifstat.getRxDropped());
				builder.append("\n发送时丢弃的包裹数: ").append(ifstat.getTxDropped());
			}
		}catch(Throwable e){
			LOG.error("", e);
			return false;
		}
		return true;
	}

}
