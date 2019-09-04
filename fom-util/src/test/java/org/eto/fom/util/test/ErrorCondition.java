package org.eto.fom.util.test;

public class ErrorCondition {
	private String name; // 姓名
	private String idCard; // 身份证
	private String status; // 错误状态
	private String message; // 错误信息
 
	ErrorCondition(String name,String idCard,String status,String message){
		this.name = name;
		this.idCard = idCard;
		this.status = status;
		this.message = message;
	}
	
	public String getName() {
		return name;
	}
 
	public void setName(String name) {
		this.name = name;
	}
 
	public String getIdCard() {
		return idCard;
	}
 
	public void setIdCard(String idCard) {
		this.idCard = idCard;
	}
 
	public String getStatus() {
		return status;
	}
 
	public void setStatus(String status) {
		this.status = status;
	}
 
	public String getMessage() {
		return message;
	}
 
	public void setMessage(String message) {
		this.message = message;
	}
 
}
