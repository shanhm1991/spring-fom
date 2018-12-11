package com.fom.context;

/**
 * 
 * @author shanhm1991
 *
 */
interface IConfig {

	String TYPE_IMPORTER = "importer";

	String TYPE_UPLOADER = "uploader";

	String TYPE_DOWNLOADER = "downloader";

	String NAME_IMPORTER = "导入";

	String NAME_UPLOADER = "上传";

	String NAME_DOWNLOADER = "下载";


	String getType();

	String getTypeName();
	
	String getSrcPath();

	String getExecutorClass();

	int getExecutorMin();

	int getExecutorMax();

	int getExecutorAliveTime();

	int getExecutorOverTime();

	boolean getExecutorCancelOnOverTime();

	long getCronTime();

	boolean matchSrc(String srcName);
}
