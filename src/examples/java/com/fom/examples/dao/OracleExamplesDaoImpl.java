package com.fom.examples.dao;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.support.SqlSessionDaoSupport;

import com.fom.examples.bean.ExamplesBean;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public class OracleExamplesDaoImpl extends SqlSessionDaoSupport implements ExamplesDao {

	@Override
	public List<Map<String,String>> selectDemo() {
		return getSqlSession().selectList("oracle.demo.selectDemo");
	}

	@Override
	public int inserDemo(ExamplesBean bean) {
		 return getSqlSession().insert("oracle.demo.insertDemo", bean);
	}

	@Override
	public int batchInsertDemo(List<ExamplesBean> list) {
		 return getSqlSession().insert("oracle.demo.batchInsertDemo", list);
	}

}
