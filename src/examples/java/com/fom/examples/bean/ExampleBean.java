package com.fom.examples.bean;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class ExampleBean {
	
	private String id;
	
	private String name;
	
	private String source;
	
	private String fileType;
	
	private String importWay;
	
	public ExampleBean(String data){
		String[] array = data.split("#"); 
		this.id = array[0];
		this.name = array[1];
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

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getImportWay() {
		return importWay;
	}

	public void setImportWay(String importWay) {
		this.importWay = importWay;
	}
}
