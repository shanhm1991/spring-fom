package com.fom.context;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.fom.context.config.HdfsZipDownloaderConfig;
import com.fom.context.executor.Executor;
import com.fom.context.executor.ZipDownloader;
import com.fom.context.helper.HdfsHelper;
import com.fom.context.helper.Helper;
import com.fom.util.HdfsUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class HdfsZipDownloader<E extends HdfsZipDownloaderConfig> extends Context<E> {
	
	protected HdfsZipDownloader(String name, String path) {
		super(name, path);
	}

	@Override
	protected void exec(E config) throws Exception {
		List<String> pathList = HdfsUtil.listPath(config.getFs(), srcPath, new PathFilter(){
			@Override
			public boolean accept(Path path) {
				if(StringUtils.isBlank(config.getSignalFile())){
					return true;
				}
				return ! config.getSignalFile().equals(path.getName());
			}
		}); 
		Helper helper = new HdfsHelper(config.getFs(), config.isDelSrc());
		Executor executor = new ZipDownloader(name, pathList, srcName, config.getDestPath(), 
				config.getEntryMax(), config.getSizeMax(), config.isWithTemp(), config.isDelSrc(), helper);
		executor.exec();
	}
	
}
