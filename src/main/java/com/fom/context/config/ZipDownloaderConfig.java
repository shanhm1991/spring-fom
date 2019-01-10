package com.fom.context.config;

import org.apache.log4j.helpers.OptionConverter;

import com.fom.util.XmlUtil;

/**
 * downloader.zip.entry.max 打包zip文件的最大entry个数<br>
 * downloader.zip.size.max  打包zip文件的最大size<br>
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class ZipDownloaderConfig extends DownloaderConfig {

	int entryMax;

	long sizeMax;

	protected ZipDownloaderConfig(String name) {
		super(name);
	}

	@Override
	void load() throws Exception {
		super.load();
		entryMax = XmlUtil.getInt(element, "downloader.zip.entry.max", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
		String strSize = XmlUtil.getString(element, "downloader.zip.size.max", "10GB");
		sizeMax = OptionConverter.toFileSize(strSize, 1024*1024*1024 * 10L);
	}
	
	@Override
	boolean valid() throws Exception {
		if(!super.valid()){
			return false;
		}
		//...
		return true;
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof ZipDownloaderConfig)){
			return false;
		}
		if(o == this){
			return true;
		}
		
		ZipDownloaderConfig c = (ZipDownloaderConfig)o; 
		if(!super.equals(c)){
			return false;
		}
		
		return entryMax == c.entryMax
				&& sizeMax == c.sizeMax;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\ndownloader.zip.entry.max=" + entryMax);
		builder.append("\ndownloader.zip.size.max=" + sizeMax);
		return builder.toString();
	}

	public final int getEntryMax() {
		return entryMax;
	}

	public final long getSizeMax() {
		return sizeMax;
	}
	
}
