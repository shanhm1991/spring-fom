package com.examples.dao;

import java.util.List;
import java.util.Map;

import com.examples.bean.ExampleBean;

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
