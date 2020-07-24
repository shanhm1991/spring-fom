package example.fom.context.download;

import org.eto.fom.context.annotation.FomContext;
import org.eto.fom.context.core.Context;

/**
 * 
 * @author shanhm
 *
 */
@FomContext(remark="扫描下载Hdfs指定目录下的目录并打包成zip", stopWithNoCron=true)
public class DownloadHdfsZipExample extends Context {

//	private String masterUrl;
//
//	private String slaveUrl;
//
//	private String srPath = "/test";
//
//	private String dest;
//
//	private boolean isDelSrc = false; 
//
//	private int entryMax = 10;
//
//	private long sizeMax = 100 * 1024 * 1024L;
//
//	private String signalName;
//
//	public DownloadHdfsZipExample(){
//		dest = new File("").getAbsolutePath() + "/download/" + name;
//	}
//
//	public DownloadHdfsZipExample(String name){
//		super(name);
//		dest = new File("").getAbsolutePath() + "/download/" + name;
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	protected Set<DownloadZipTask> scheduleBatchTasks() throws Exception {
//		final FileSystem fs = HdfsUtil.getFileSystem(masterUrl, slaveUrl);
//		List<String> dirList = HdfsUtil.list(masterUrl, slaveUrl, new Path(srPath), new PathFilter(){
//			@Override
//			public boolean accept(Path path) {
//				if(!PatternUtil.match("regex", path.getName())){
//					return false;
//				}
//
//				FileStatus[] subArray = null;
//				try {
//					subArray = fs.listStatus(path);
//				} catch (Exception e) {
//					log.error("", e);
//					return false;
//				}
//				if(ArrayUtils.isEmpty(subArray)){
//					return false;
//				}
//				if(StringUtils.isBlank(signalName)){
//					return true;
//				}
//
//				for (FileStatus sub : subArray){
//					if(signalName.equals(sub.getPath().getName())){
//						return true;
//					}
//				}
//				return false;
//			}
//		});
//		
//		Set<DownloadZipTask> set = new HashSet<>();
//		for(String dir : dirList){
//			List<String> pathList = HdfsUtil.list(masterUrl, slaveUrl, new Path(dir), new PathFilter(){
//				@Override
//				public boolean accept(Path path) {
//					if(StringUtils.isBlank(signalName)){
//						return true;
//					}
//					return ! signalName.equals(path.getName());
//				}
//			});  
//			
//			HdfsHelper helper = new HdfsHelper(masterUrl, slaveUrl);
//			String sourceName = new File(dir).getName();
//			DownloadHdfsZipExampleResultHandler handler = 
//					new DownloadHdfsZipExampleResultHandler(name, masterUrl, slaveUrl, srPath,isDelSrc);
//			set.add(new DownloadZipTask(pathList, sourceName, dest, entryMax, sizeMax, isDelSrc, helper, handler));
//		}
//		return set;
//	}

}
