package com.fom.modules.importer.demo.sp;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fom.context.ZipImporter;
import com.fom.modules.importer.demo.wa.WAConfig;
import com.fom.util.StorageUtil;
import com.fom.util.db.handler.OraHandler;

/**
 * 敏感图片
 */
public class SPImporter extends ZipImporter<WAConfig, Map<String,Object>> {
	
	private static final String POOL_ORA = "scloudrs";

	private static final String SQL = "insert into NCRS_SENSITIVEPIC("
			+ "MSGID, APPTYPE, RESOURCETYPE, SENDUSERID, SENDAPPID,"
			+ "RECEIVERID, RECEIVERTYPE, RECEIVERACCOUNT, CAPTURETIME,SENDIP,"
			+ "SENDPORT, TRANSFILE, MAINFILE, FILESIZE, SENDIPID,"
			+ "SENDAREACODE, PICTYPE, SORT_ORDER, STAT_BYGROUP, GROUP_SELECTED,"
			+ "PIC_INDEX, PICTURE_TAGS, PICTURE_SCORE, CONTENTTEXT, RULEID," + "INSERTTIME) values("
			+ "#MSGID#, '1030036', 'WA_SOURCE_0048', #SENDUSERID#, #SENDAPPID#,"
			+ "#RECEIVERID#, '2', '', #CAPTURETIME#, #SENDIP#," + "'', #TRANSFILE#, #MAINFILE#, #FILESIZE#, #SENDIPID#,"
			+ "#SENDAREACODE#, #PICTYPE#, #SORT_ORDER#, #STAT_BYGROUP#, #GROUP_SELECTED#,"
			+ "#PIC_INDEX#, #PICTURE_TAGS#, #PICTURE_SCORE#, #CONTENTTEXT#, #RULEID#," + "#INSERTTIME#)";

	protected SPImporter(String name, String path) {
		super(name, path);
	}

	@Override
	protected boolean validZipContent(WAConfig config, List<String> nameList) {
		int count = 1;
		for(String name : nameList){
			if(name.endsWith(".bcp")){
				count++;
			}
		}
		if(count != 1){
			log.warn("文件校验失败, bcp文件有且仅能有一个");
			return false;
		}
		return true;
	}

	@Override
	protected void praseLineData(WAConfig config, List<Map<String, Object>> lineDatas, String line, long batchTime)
			throws Exception {
		String[] array = line.split("\t");
		if (array.length < 21) {
			log.warn("忽略行数据,字段数小于21:");
			return;
		}
		String code = array[12];
		if (code.length() < 10) {
			log.warn("忽略行数据，areacode长度小于10");
			return;
		}
		code = code.substring(4, 10);

		// url
		String url = "";
		File pic = new File(unzipDir + File.separator + array[10]);
		if(pic.exists()){
			url = StorageUtil.storageFile(pic, config.getZkAddress(), true);
			url = url.replaceAll(config.getFullTextUrl(), "");
		}

		String msgid = array[0];
		if (msgid.length() > 64) {
			msgid = msgid.substring(0, 64);
		}

		String contenttext = array[20];
		if (contenttext.length() > 4000) {
			contenttext = contenttext.substring(0, 4000);
		}

		Map<String, Object> map = new HashMap<String, Object>();
		try {
			map.put("CAPTURETIME", Long.valueOf(array[7]));
			map.put("SENDIP", Long.valueOf(array[8]));
			map.put("FILESIZE", Integer.valueOf(array[11]));
			map.put("PICTYPE", Integer.valueOf(array[13]));
			map.put("SORT_ORDER", Integer.valueOf(array[14]));
			map.put("STAT_BYGROUP", Integer.valueOf(array[15]));
			map.put("GROUP_SELECTED", Integer.valueOf(array[16]));
			map.put("PIC_INDEX", Integer.valueOf(array[17]));
		} catch (Exception e) {
			log.warn("忽略行数据,数据转换失败");
			return;
		}
		map.put("MSGID", msgid);
		map.put("SENDUSERID", array[3]);
		map.put("SENDAPPID", array[4]);
		map.put("RECEIVERID", array[5]);
		map.put("TRANSFILE", url);
		map.put("MAINFILE", array[10]);
		map.put("SENDIPID", array[12]);
		map.put("SENDAREACODE", code);
		map.put("PICTURE_TAGS", array[18]);
		map.put("PICTURE_SCORE", array[19]);
		map.put("CONTENTTEXT", contenttext);
		map.put("RULEID", array[21]);
		map.put("INSERTTIME", batchTime / 1000);
		lineDatas.add(map);
	}

	@Override
	protected void batchProcessLineData(WAConfig config, List<Map<String, Object>> lineDatas, long batchTime)
			throws Exception {
		OraHandler.defaultHandler.batchExecute(POOL_ORA, SQL, lineDatas);
		log.info("批处理结束[" + lineDatas.size() + "], 耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
	}
}
