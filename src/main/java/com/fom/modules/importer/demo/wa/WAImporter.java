package com.fom.modules.importer.demo.wa;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fom.context.ZipImporter;
import com.fom.util.StorageUtil;
import com.fom.util.db.handler.OraHandler;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
public class WAImporter extends ZipImporter<WAConfig, Map<String,Object>> {
	
	private static final String POOL_ORA_scloudrs = "scloudrs";
	
	private static final String POOL_ORA_nbpf = "nbpf";

	private static final String SQL = "insert into ZDR_WARN_ACTION(UUID,"
			+ "ENTITY_ID, ENTITY_TYPE, WARNCODE, TITLE, APPTYPE,"
			+ "GROUPCODE, APPID, CONTENT, IP, CITYCODE,"
			+ "SENDTIME, EMOTION, FILESIZE, FILENAME, FILETYPE,"
			+ "MAINFILE, CAPTURE_TIME, DATA_SOURCE, MODEL_TYPE, intelligence_id,"
			+ "MSG_TYPE) values(SEQ_WARN_ACTION.NEXTVAL,"
			+ "'', '', '', '', '1030036',"
			+ "#GROUPCODE#, #APPID#, #CONTENT#, #IP#, #CITYCODE#,"
			+ "#SENDTIME#, '1', #FILESIZE#, #FILENAME#, #FILETYPE#,"
			+ "#MAINFILE#, #CAPTURE_TIME#, '', '10101', '', 2)";

	private static final String SQL_query = "select UNIT from BASE_IPLIBRARYTAB_GLOBE where FROMIP <= #FROMIP# and TOIP >= #TOIP#";

	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	protected WAImporter(String name, String path) {
		super(name, path);
	}

	@Override
	protected boolean validContents(WAConfig config, List<String> nameList) {
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
	protected void praseLineData(WAConfig config, List<Map<String,Object>> lineDatas, String line, long batchTime) throws Exception {
		String[] array = line.split("\t");
		if(array.length < 23){
			log.warn("忽略行数据,字段数小于23:");
			return;
		}

		String url = "";
		File pic = new File(unzipDir + File.separator + array[22]);
		if(pic.exists() && pic.isFile()){
			url = StorageUtil.storageFile(pic, config.getZkAddress(), true);
			url = url.replaceAll(config.getFullTextUrl(), "");
		}

		Map<String,Object> map = new HashMap<String,Object>();
		map.put("GROUPCODE", array[6]);
		map.put("APPID", array[8]);
		map.put("CONTENT", array[15]);
		map.put("IP", array[10]);
		map.put("SENDTIME", array[9]);
		map.put("FILESIZE", array[18]);
		map.put("FILENAME", array[22]);
		map.put("FILETYPE", array[13]);
		map.put("MAINFILE", url);
		map.put("CITYCODE", getArea(praseIp(array[10]), config));
		map.put("CAPTURE_TIME", praseTime(array[9]));
		lineDatas.add(map);
	}

	@Override
	protected void batchProcessLineData(WAConfig config, List<Map<String,Object>> lineDatas, long batchTime) throws Exception{ 
		OraHandler.defaultHandler.batchExecute(POOL_ORA_scloudrs, SQL, lineDatas);
	}

	private long praseTime(String datestring){
		long time = 0L;
		try{
			time = FORMAT.parse(datestring).getTime() / 1000;
		}catch (Exception e){
			log.warn("CAPTURE_TIME获取异常", e);
		}
		return time;
	}

	private String getArea(long ip, WAConfig config) throws Exception{
		String area = "";
		if(ip == 0){
			return area;
		}

		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("FROMIP", ip);
		paramMap.put("TOIP", ip);
		List<Map<String, Object>> list = OraHandler.defaultHandler.queryForList(POOL_ORA_nbpf, SQL_query, paramMap);
		if(list.isEmpty()){
			log.warn("CITYCODE获取查询为空");
			return area;
		}
		area = String.valueOf(list.get(0).get("UNIT"));
		return area;
	}

	private long praseIp(String ipStr) {
		try {
			if (ipStr == null || ipStr.length() == 0){
				return 0;
			}
			long addr = 0;
			String b = "";
			int tmpFlag1 = 0, tmpFlag2 = 0;
			tmpFlag1 = 0;
			tmpFlag2 = ipStr.indexOf(".");
			b = ipStr.substring(tmpFlag1, tmpFlag2);
			addr = new Long(b).longValue();
			tmpFlag1 = tmpFlag2 + 1;
			tmpFlag2 = ipStr.indexOf(".", tmpFlag1);
			addr = addr << 8;
			b = ipStr.substring(tmpFlag1, tmpFlag2);
			addr += new Long(b).longValue();
			tmpFlag1 = tmpFlag2 + 1;
			tmpFlag2 = ipStr.indexOf(".", tmpFlag1);
			addr = addr << 8;
			b = ipStr.substring(tmpFlag1, tmpFlag2);
			addr += new Long(b).longValue();
			addr = addr << 8;
			addr += new Long(ipStr.substring(tmpFlag2 + 1, ipStr.length())).longValue();
			return addr;
		} catch (Exception e) {
			log.warn("CITYCODE转换ip异常", e);
		}
		return 0;
	}
}
