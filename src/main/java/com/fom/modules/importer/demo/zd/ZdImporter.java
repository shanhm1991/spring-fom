package com.fom.modules.importer.demo.zd;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fom.context.ImporterConfig;
import com.fom.context.Importer;
import com.fom.util.db.handler.OraHandler;

/**
 * 重点内容
 */
public class ZdImporter extends Importer<ImporterConfig, Map<String, Object>> {
	
	private static final String POOL_ORA = "zdurl";

	private static final String SQL = "insert into NCDATA_ZDURL_DETAIL values(#v1#, #v2#, #v3#, #v4#, #v5#, #v6#, #v7#, #v8#, #v9#, #v10#,#v11#)";

	private static final String SQL_merge = "merge into NCDATA_ZDURL T1 USING ("
			+ "select /*+index(NCDATA_ZDURL_DETAIL,IDX_NCDATA_ZDURL_DETAIL_TAG) no_index(NCDATA_ZDURL_DETAIL,IDX_NCDATA_ZDURL_DETAIL_URL)*/ url,count(1) cnt ,"
			+ "max(URL_TYPE) keep(dense_rank last order by sendtime) URL_TYPE,max(userid) keep(dense_rank last order by sendtime) userid,"
			+ "max(groupid) keep(dense_rank last order by sendtime) groupid, " + "max(sendtime) lasttime,"
			+ "min(sendtime) firsttime, min(userid) keep(dense_rank last order by sendtime) userid1,"
			+ "min(groupid) keep(dense_rank last order by sendtime) groupid1,"
			+ "min(sign) keep(dense_rank last order by sendtime) sign "
			+ "from NCDATA_ZDURL_DETAIL  where tag=#time# and insert_time=#time1# group by url)"
			+ " T2 on (T1.url = t2.url) "
			+ "when matched then "
			+ "update set T1.find_times= T1.find_times + T2.cnt," + "T1.last_userid = T2.USERID,"
			+ "T1.LAST_GROUPID = T2.GROUPID," + "T1.LAST_TIME = T2.lasttime " 
			+ "WHEN NOT matched THEN "
			+ "INSERT(URL, URL_TYPE, FIND_TIMES, FIRST_TIME, FIRST_USERID, FIRST_GROUPID,LAST_TIME, LAST_USERID, LAST_GROUPID, SIGN) "
			+ "VALUES(T2.URL, T2.URL_TYPE, T2.cnt, T2.firsttime, T2.USERID1, T2.GROUPID1, T2.lasttime, T2.USERID, T2.GROUPID, T2.SIGN)";

	private static final String SQL_NAME = "select NAME from NAME_DICTIONARY";

	private static final String SPLIT = "\u0019";

	List<String> nameList = new ArrayList<String>();

	protected ZdImporter(String name, String path) {
		super(name, path);
	}

	@Override
	protected void executeBefore(ImporterConfig config) throws Exception {
		try {
			List<Map<String, Object>> name = 
					OraHandler.defaultHandler.queryForList(POOL_ORA, SQL_NAME, null);
			for(int i=0; i<name.size(); i++) {
				for(Map.Entry<String, Object> entry : name.get(i).entrySet()) {
					nameList.add((String) entry.getValue());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void praseLineData(ImporterConfig config, List<Map<String, Object>> lineDatas, String line, long batchTime) {
		String[] array = line.split("\t", -1);
		if(array.length < 16){
			log.warn("忽略行失败,字段数小于16");
			return;
		}

		Date date = new Date(batchTime);
		// 手机号码
		if (StringUtils.isNotBlank(array[F_PHONE_POSITION])) {
			String[] content = array[F_PHONE_POSITION].split(SPLIT);
			for (String str : content) {
				addParamMap(array, str, URL_TYPE_F_PHONE, date, batchTime, lineDatas);
			}
		}

		// 身份证
		if (StringUtils.isNotBlank(array[F_IDCARD_POSITION])) {
			String[] content = array[F_IDCARD_POSITION].split(SPLIT);
			for (String str : content) {
				addParamMap(array, str, URL_TYPE_F_IDCARD, date, batchTime, lineDatas);
			}
		}

		// 银行卡
		if (StringUtils.isNotBlank(array[F_BANKCARD_POSITION])) {
			String[] content = array[F_BANKCARD_POSITION].split(SPLIT);
			for (String str : content) {
				addParamMap(array, str, URL_TYPE_F_BANKCARD, date, batchTime, lineDatas);
			}
		}

		// 邮箱
		if (StringUtils.isNotBlank(array[F_EMAIL_POSITION])) {
			String[] content = array[F_EMAIL_POSITION].split(SPLIT);
			for (String str : content) {
				addParamMap(array, str, URL_TYPE_F_EMAIL, date, batchTime, lineDatas);
			}
		}

		// 链接
		if (StringUtils.isNotBlank(array[F_LINK_POSITION])) {
			String[] content = array[F_LINK_POSITION].split(SPLIT);
			for (String str : content) {
				addParamMap(array, str, URL_TYPE_F_LINK, date, batchTime, lineDatas);
			}
		}

		// QQ号
		if (StringUtils.isNotBlank(array[F_QQ_POSITION])) {
			String[] content = array[F_QQ_POSITION].split(SPLIT);
			for (String str : content) {
				addParamMap(array, str, URL_TYPE_F_QQ, date, batchTime, lineDatas);
			}
		}

		// 姓名
		if (StringUtils.isNotBlank(array[F_NAME_POSITION])) {
			String[] content = array[F_NAME_POSITION].split(SPLIT);
			if(nameList.contains(array[F_NAME_POSITION].trim().substring(0, 1))) {
				for (String str : content) {
					addParamMapSign(array, str, URL_TYPE_F_NAME, date, batchTime, lineDatas);
				}
			}else{
				for (String str : content) {
					addParamMap(array, str, URL_TYPE_F_NAME, date, batchTime, lineDatas);
				}
			}
		}

		// 微信号
		if (StringUtils.isNotBlank(array[F_WXACCOUNT_POSITION])) {
			String[] content = array[F_WXACCOUNT_POSITION].split(SPLIT);
			for (String str : content) {
				addParamMap(array, str, URL_TYPE_F_WXACCOUNT, date, batchTime, lineDatas);
			}
		}
	}

	//batch(insert + merge)
	@Override
	protected void batchProcessLineData(ImporterConfig config, List<Map<String, Object>> lineDatas, long batchTime) throws Exception {
		OraHandler.defaultHandler.startTransaction(POOL_ORA);
		try{
			OraHandler.defaultHandler.batchExecute(POOL_ORA, SQL, lineDatas);
			Date date = new Date(batchTime);
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("time", batchTime);
			paramMap.put("time1", date);
			OraHandler.defaultHandler.execute(POOL_ORA, SQL_merge, paramMap);
		}catch(Exception e){
			Throwable th = e.getCause();
			if(th != null && th instanceof SQLException){
				SQLException se = (SQLException)th;
				if(se.getSQLState().equals("00060")){ //deadlock重试
					throw e;
				}else{
					log.error("忽略数据异常", e); 
				}
			}
		}finally{
			OraHandler.defaultHandler.endTransaction(POOL_ORA);
		}
		log.info("批处理结束[" + lineDatas.size() + "], 耗时=" + (System.currentTimeMillis() - batchTime) + "ms");
	}

	private void addParamMap(String[] array, String v1, int v2, Date date, long time, List<Map<String, Object>> paramMaps) {
		if(StringUtils.isBlank(v1)){
			return;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("v1", v1);
		map.put("v2", v2);
		map.put("v3", array[P_DATATYPE]);
		map.put("v4", array[P_GROUPID]);
		map.put("v5", array[P_USERID]);
		map.put("v6", array[P_SENDTIME]);
		map.put("v7", array[P_SENDIP]); 
		map.put("v8", date);
		map.put("v9", time);
		map.put("v10", subcontent(array[CONTENT_POSITION], 1000, v1));
		map.put("v11", "2");
		paramMaps.add(map);
	}

	private void addParamMapSign(String[] array, String v1, int v2, Date date, long time, List<Map<String, Object>> paramMaps) {
		if(StringUtils.isBlank(v1)){
			return;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("v1", v1);
		map.put("v2", v2);
		map.put("v3", array[P_DATATYPE]);
		map.put("v4", array[P_GROUPID]);
		map.put("v5", array[P_USERID]);
		map.put("v6", array[P_SENDTIME]);
		map.put("v7", array[P_SENDIP]); 
		map.put("v8", date);
		map.put("v9", time);
		map.put("v10", subcontent(array[CONTENT_POSITION], 1000, v1));
		map.put("v11", "1");
		paramMaps.add(map);
	}

	/**
	 * 截取聊天内容超过4000字节的字段
	 * 
	 * @param s
	 * @param num
	 * @param url
	 * @return
	 */
	private String subcontent(String s, int num, String url) {
		int changdu = s.getBytes().length;
		String back = null;
		String front = null;
		if (changdu > num) {
			back = s.substring(0, s.length() - 1);
			if (back.contains(url)) {
				s = subcontent(back, num, url);
			} else {
				front = s.substring(1, s.length());
				s = subcontent(front, num, url);
			}
		}
		return s;
	}

	public static final int URL_TYPE_F_PHONE = 1;

	public static final int URL_TYPE_F_IDCARD = 2;

	public static final int URL_TYPE_F_BANKCARD = 3;

	public static final int URL_TYPE_F_EMAIL = 4;

	public static final int URL_TYPE_F_LINK = 5;

	public static final int URL_TYPE_F_QQ = 6;

	public static final int URL_TYPE_F_NAME = 7;

	public static final int URL_TYPE_F_WXACCOUNT = 8;

	public static final String ENCODING = "UTF-8";

	public static final int SYS_LIMITS = 10;

	public static final int DATA_SIZE = 16;

	public static final int P_DATATYPE = 0;

	public static final int MSGID_POSITION = 1;

	public static final int P_GROUPID = 2;

	public static final int P_USERID = 3;

	public static final int ACCOUNTNAME_POSITION = 4;

	public static final int P_SENDTIME = 5;

	public static final int P_SENDIP = 6;

	public static final int F_PHONE_POSITION = 7;

	public static final int F_IDCARD_POSITION = 8;

	public static final int F_BANKCARD_POSITION = 9;

	public static final int F_EMAIL_POSITION = 10;

	public static final int F_LINK_POSITION = 11;

	public static final int F_QQ_POSITION = 12;

	public static final int F_NAME_POSITION = 13;

	public static final int F_WXACCOUNT_POSITION = 14;

	public static final int CONTENT_POSITION = 15;

}
