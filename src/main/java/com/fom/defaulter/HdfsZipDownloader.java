package com.fom.defaulter;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.fom.context.Context;
import com.fom.context.executor.ZipDownloader;
import com.fom.context.executor.helper.HdfsZipDownloaderHelper;
import com.fom.util.HdfsUtil;

/**
 * 
 * @author shanhm
 *
 */
public class HdfsZipDownloader<E extends HdfsZipDownloaderConfig> extends Context<E> {

	private HdfsZipDownloaderHelper helper;

	protected HdfsZipDownloader(String name, String path) {
		super(name, path);
	}

	@Override
	protected final void exec(E config) throws Exception {
		helper = new HdfsZipDownloaderHelper(config.getFs());
		List<String> pathList = HdfsUtil.listPath(config.getFs(), sourceUri, new PathFilter(){
			@Override
			public boolean accept(Path path) {
				if(StringUtils.isBlank(config.getSignalFileName())){
					return true;
				}
				return ! config.getSignalFileName().equals(path.getName());
			}
		}); 
		
		String sourceName = new File(sourceUri).getName();
		ZipDownloader zipDownloader = new ZipDownloader(name, sourceName, pathList, config.getDestPath(), 
				config.getEntryMax(), config.getSizeMax(), config.isDelSrc(), helper);
		if(zipDownloader.call() && config.isDelSrc() && !helper.delete(sourceUri)){
			log.error("删除源目录失败."); 
		}
	}
}
