package com.fom.context;

import java.util.Map;

public interface ManagerService {

	Map<String, Map<String,String>> list() throws Exception;
	
	String detail(String name) throws Exception;
	
	String xml(String name) throws Exception;
	
	String apply(String name, String data) throws Exception;
	
	String stop(String name) throws Exception;
	
	String stopAll() throws Exception;
	
	String start(String name) throws Exception;
	
	String startAll() throws Exception;
	
	String restart(String name) throws Exception;
	
	String restartAll() throws Exception;
	
	Map<String,Object> srcs(String name, boolean match) throws Exception;
}
