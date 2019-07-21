package com.examples.dao;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.support.SqlSessionDaoSupport;

import com.examples.bean.ExampleBean;

/**
 * 
 * @author shanhm
 *
 */
public class MysqlExamplesDaoImpl extends SqlSessionDaoSupport implements ExamplesDao {
	
	@Override
	public List<Map<String,String>> select() {
		return getSqlSession().selectList("org.mybatis.mysqlExample.select");
	}

	@Override
	public int insert(ExampleBean bean) {
		 return getSqlSession().insert("org.mybatis.mysqlExample.insert", bean);
	}

	@Override
	public int batchInsert(List<ExampleBean> list) {
		 return getSqlSession().insert("org.mybatis.mysqlExample.batchInsert", list);
	}

}
