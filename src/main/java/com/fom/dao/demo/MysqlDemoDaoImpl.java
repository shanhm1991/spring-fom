package com.fom.dao.demo;

import java.util.List;

import org.mybatis.spring.support.SqlSessionDaoSupport;

import com.fom.modules.importer.demo.DemoBean;

public class MysqlDemoDaoImpl extends SqlSessionDaoSupport implements MysqlDemoDao {
	
	@Override
	public List<DemoBean> selectDemo() {
		return getSqlSession().selectList("mysql.demo.selectDemo");
	}

	@Override
	public int inserDemo(DemoBean bean) {
		 return getSqlSession().insert("mysql.demo.insertDemo", bean);
	}

	@Override
	public int batchInsertDemo(List<DemoBean> list) {
		 return getSqlSession().insert("mysql.demo.batchInsertDemo", list);
	}

}
