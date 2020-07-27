package example.fom.fomschedulbatch.mybatis.mapper;

import java.util.List;

import example.fom.fomcontextxml.mybatis.bean.ExampleBean;

/**
 * 
 * @author shanhm
 *
 */
public interface InputMysqMapper {

	void input(List<ExampleBean> list);
}
