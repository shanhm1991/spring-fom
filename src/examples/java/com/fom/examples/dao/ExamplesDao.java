package com.fom.examples.dao;

import java.util.List;
import java.util.Map;

import com.fom.examples.bean.ExamplesBean;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public interface ExamplesDao {
	
	List<Map<String,String>> selectDemo();
	
	int inserDemo(ExamplesBean bean);
	
	int batchInsertDemo(List<ExamplesBean> list);

}
