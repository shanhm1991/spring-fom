package example.fom.fomcontextxml.mybatis.bean;

import java.util.List;

/**
 * 
 * @author shanhm
 *
 */
public class ExampleBean {
	
	private String id;
	
	private String name;
	
	private String source;
	
	private String fileType;
	
	private String importWay;
	
	public ExampleBean(List<String> columns){
		this.id = columns.get(0); 
		this.name = columns.get(1);
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

	@Override
	public String toString() {
		return "{id=" + id + ", name=" + name + ", source=" + source + ", fileType=" + fileType + ", importWay=" + importWay + "}";
	}
}