package com.fom.context;

import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
@Service(value="managerService")
public class ManagerServiceImpl implements ManagerService {

	@Override
	public Map<String, Config> getConfigMap() {
		return ConfigManager.getConfigMap();
	}
}
