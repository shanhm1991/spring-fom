package com.fom.modules.importer.demo;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class DemoBean {
	
	private String id;
	
	private String name;
	
	private String age;
	
	private String tel;
	
	public DemoBean(String data){
		String[] array = data.split("#");
		this.id = array[0];
		this.name = array[1];
		this.age = array[2];
		this.tel = array[3];
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}

	public String getTel() {
		return tel;
	}

	public void setTel(String tel) {
		this.tel = tel;
	}

}
