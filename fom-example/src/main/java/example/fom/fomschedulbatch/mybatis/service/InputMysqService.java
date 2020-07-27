package example.fom.fomschedulbatch.mybatis.service;

import java.util.List;

import example.fom.fomcontextxml.mybatis.bean.ExampleBean;

/**
 * 
 * @author shanhm
 *
 */
public interface InputMysqService {

	void input(List<ExampleBean> list);
}
