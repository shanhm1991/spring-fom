package com.fom.examples.dao;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.support.SqlSessionDaoSupport;

import com.fom.examples.bean.ExampleBean;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public class OracleExamplesDaoImpl extends SqlSessionDaoSupport implements ExamplesDao {

	@Override
	public List<Map<String,String>> select() {
		return getSqlSession().selectList("oracleExample.select");
	}

	@Override
	public int insert(ExampleBean bean) {
		 return getSqlSession().insert("oracleExample.insert", bean);
	}

	@Override
	public int batchInsert(List<ExampleBean> list) {
		 return getSqlSession().insert("oracleExample.batchInsert", list);
	}

}
