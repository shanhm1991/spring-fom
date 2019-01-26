package com.fom.context.helper;

import java.io.File;

import org.apache.hadoop.fs.FileSystem;

public class HdfsZipDownloaderHelper extends HdfsDownloaderHelper implements ZipDownloaderHelper {

	public HdfsZipDownloaderHelper(FileSystem fs) {
		super(fs);
	}

	@Override
	public String getSourceName(String sourceUri) {
		return new File(sourceUri).getName();
	}

}
