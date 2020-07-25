package example.fom.batchschedul.dao;

import java.util.List;
import java.util.Map;

import example.fom.xml.mybatis.bean.ExampleBean;

/**
 * 
 * @author shanhm
 *
 */
public interface ExamplesDao {
	
	List<Map<String,String>> select();
	
	int insert(ExampleBean bean);
	
	int batchInsert(List<ExampleBean> list);

}
