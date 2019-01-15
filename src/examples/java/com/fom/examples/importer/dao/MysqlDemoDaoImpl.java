package com.fom.examples.importer.dao;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.support.SqlSessionDaoSupport;

import com.fom.examples.importer.DemoBean;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public class MysqlDemoDaoImpl extends SqlSessionDaoSupport implements DemoDao {
	
	@Override
	public List<Map<String,String>> selectDemo() {
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
