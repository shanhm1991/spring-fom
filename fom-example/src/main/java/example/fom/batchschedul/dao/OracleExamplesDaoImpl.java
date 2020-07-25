package example.fom.batchschedul.dao;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.support.SqlSessionDaoSupport;

import example.fom.xml.mybatis.bean.ExampleBean;

/**
 * 
 * @author shanhm
 *
 */
public class OracleExamplesDaoImpl extends SqlSessionDaoSupport implements ExamplesDao {

	@Override
	public List<Map<String,String>> select() {
		return getSqlSession().selectList("org.mybatis.oracleExample.select");
	}

	@Override
	public int insert(ExampleBean bean) {
		 return getSqlSession().insert("org.mybatis.oracleExample.insert", bean);
	}

	@Override
	public int batchInsert(List<ExampleBean> list) {
		 return getSqlSession().insert("org.mybatis.oracleExample.batchInsert", list);
	}

}
