package com.fom.examples.importer.dao;

import java.util.List;
import java.util.Map;

import com.fom.examples.importer.DemoBean;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public interface DemoDao {
	
	List<Map<String,String>> selectDemo();
	
	int inserDemo(DemoBean bean);
	
	int batchInsertDemo(List<DemoBean> list);

}
