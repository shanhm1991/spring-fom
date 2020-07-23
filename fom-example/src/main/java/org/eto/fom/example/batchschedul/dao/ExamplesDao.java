package org.eto.fom.example.batchschedul.dao;

import java.util.List;
import java.util.Map;

import org.eto.fom.example.batchschedul.ExampleBean;

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
