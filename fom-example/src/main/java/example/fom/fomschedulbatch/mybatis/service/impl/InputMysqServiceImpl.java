package example.fom.fomschedulbatch.mybatis.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import example.fom.fomcontextxml.mybatis.bean.ExampleBean;
import example.fom.fomcontextxml.mybatis.mapper.InputOracleMapper;
import example.fom.fomschedulbatch.mybatis.service.InputMysqService;

/**
 * 
 * @author shanhm
 *
 */
@Service
public class InputMysqServiceImpl implements InputMysqService {
	
	@Autowired
	private InputOracleMapper mapper;

	@Override
	public void input(List<ExampleBean> list) {
		mapper.input(list); 
	}

}
