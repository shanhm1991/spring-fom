package com.fom.examples.dao;

import java.util.List;
import java.util.Map;

import com.fom.examples.bean.ExampleBean;

/**
 * 
 * @author shanhm
 * @date 2019年1月15日
 *
 */
public interface ExamplesDao {
	
	List<Map<String,String>> select();
	
	int insert(ExampleBean bean);
	
	int batchInsert(List<ExampleBean> list);

}
