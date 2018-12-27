package com.fom.dao.demo;

import java.util.List;

import com.fom.modules.importer.demo.DemoBean;

public interface MysqlDemoDao {
	
	List<DemoBean> selectDemo();
	
	int inserDemo(DemoBean bean);
	
	int batchInsertDemo(List<DemoBean> list);

}
