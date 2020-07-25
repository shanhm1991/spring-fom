package example.fom.xml.mybatis.mapper;

import java.util.List;

import example.fom.xml.mybatis.bean.ExampleBean;

/**
 * 
 * @author shanhm
 *
 */
public interface InputOracleMapper {

	void input(List<ExampleBean> list);
}
