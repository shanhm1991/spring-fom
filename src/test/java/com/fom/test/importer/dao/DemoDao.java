package com.fom.test.importer.dao;

import java.util.List;
import java.util.Map;

import com.fom.test.importer.DemoBean;

public interface DemoDao {
	
	List<Map<String,String>> selectDemo();
	
	int inserDemo(DemoBean bean);
	
	int batchInsertDemo(List<DemoBean> list);

}
