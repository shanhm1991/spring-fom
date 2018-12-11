package com.fom.context;

/**
 * 
 * @author shanhm1991
 *
 * @param <E>
 */
public class HdfsDownloader<E extends HdfsDownloaderConfig> extends Executor<E> {

	protected HdfsDownloader(String name, String path) {
		super(name, path);
	}

	@Override
	void execute() throws Exception {
		config.getFs();
		
//		config.fs.copyToLocalFile(config., status.getPath(), new Path(subTempPath), true);
	}
	
	
	
//	private void fileDownload(FileStatus[] srcFiles, final HdfsConfig config) throws Exception{ 
//		for(FileStatus status : srcFiles){
//			try{
//				config.fs.copyToLocalFile(config.isSrcDel(), status.getPath(), new Path(subTempPath), true);
//				log.debug("下载文件完成(" + (status.getLen() / 1024) + "KB)：" + status.getPath().getName());
//			}catch (Exception e){
//				log.error("下载文件异常：" + status.getPath().getName(),e);
//			}
//		}
//	}

}
