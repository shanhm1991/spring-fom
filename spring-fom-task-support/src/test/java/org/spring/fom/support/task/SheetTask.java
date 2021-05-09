package org.spring.fom.support.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.spring.fom.support.task.parse.ParseSheetTask;

/**
 * 
 * @author shanhm1991@163.com
 *
 */
public class SheetTask extends ParseSheetTask<Boolean> {

	private File excel;

    public SheetTask(File excel) {
        super(excel);
        this.excel = excel;
    }
	
    @Override
    protected File findExcel() throws Exception {
        return excel;
    }
	
    @Override
    protected InputStream getExcelInputStream(String sourceUri) throws Exception {
        return new FileInputStream(excel);
    }
	
    protected void parseValue(String value, String type, String field, Map<String,Object> data, String columnName) throws Exception {
        if(StringUtils.isBlank(value)){ 
            return;
        }
		
        if("number".equals(type)){
            data.put(field, Long.valueOf(value));
        }else{ 
            data.put(field, value);
        }
    }

    @Override
    protected Boolean handlerData(Map<String, Collection<Map<String, Object>>> excelData) throws Exception {
        System.out.println(excelData);
        return true;
    } 

}

