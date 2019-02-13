package com.fom.context;

import java.util.Map;

/**
 * 
 * @author shanhm
 *
 */
public interface FomService {

	Map<String, Object> list() throws Exception;
	
	Map<String, Object> save(String name, String data) throws Exception;
	
	Map<String,Object> start(String name) throws Exception;
	
	Map<String,Object> stop(String name) throws Exception;
	
	Map<String,Object> interrupt(String name) throws Exception;
	
	Map<String,Object> state(String name) throws Exception;
	
	Map<String,Object> create(String json)  throws Exception;
	
	void changeLogLevel(String name, String level);
	
	Map<String,Object> taskdetail(String name) throws Exception;
	
}
