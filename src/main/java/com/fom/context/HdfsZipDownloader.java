package com.fom.context;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.fom.context.config.HdfsZipDownloaderConfig;
import com.fom.util.HdfsUtil;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 * @param <E>
 */
public class HdfsZipDownloader<E extends HdfsZipDownloaderConfig> extends ZipDownloader<E> {

	protected HdfsZipDownloader(String name, String path) {
		super(name, path);
	}
	
	@Override
	protected List<String> getPathList() throws Exception {
		FileStatus[] statusArray = config.getFs().listStatus(new Path(srcPath), new PathFilter(){
			@Override
			public boolean accept(Path path) {
				if(StringUtils.isBlank(config.getSignalFile())){
					return true;
				}
				return ! config.getSignalFile().equals(path.getName());
			}
		}); 

		List<String> list = new LinkedList<>();
		if(ArrayUtils.isEmpty(statusArray)){
			return list;
		}
		for(FileStatus status : statusArray){
			list.add(status.getPath().toString());
		}
		return list;
	}
	
	@Override
	protected InputStream getResourceInputStream(String path) throws Exception {
		return HdfsUtil.downloadAsStream(config.getFs(), path);
	}
	
	@Override
	protected long getResourceSize(String path) throws Exception {
		return config.getFs().getFileStatus(new Path(path)).getLen();
	}

	@Override
	protected boolean deletePath(String path) throws Exception {
		return config.getFs().delete(new Path(path), true);
	}
}
