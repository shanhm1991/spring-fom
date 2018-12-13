package com.fom.context;

import java.io.File;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fom.util.db.pool.PoolManager;

/**
 * 
 * @author X4584
 * @date 2018年12月13日
 *
 */
@Service(value="managerService")
public class ManagerServiceImpl extends PoolManager implements ManagerService {
	
	static void listen(File poolXml){
		if(!poolXml.exists()){
			return;
		}
		listenPool(poolXml);
	}

	@Override
	public Map<String, Config> getConfigMap() {
		return ConfigManager.getConfigMap();
	}
	
	
}
