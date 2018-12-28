package com.fom.dao.demo;

import java.util.List;
import java.util.Map;

import com.fom.modules.importer.demo.DemoBean;

public interface DemoDao {
	
	List<Map<String,String>> selectDemo();
	
	int inserDemo(DemoBean bean);
	
	int batchInsertDemo(List<DemoBean> list);

}
