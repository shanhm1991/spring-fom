package com.fom.context;

import org.apache.hadoop.conf.Configuration;

import com.fom.util.XmlUtil;

/**
 * <src.path>
 * <src.pattern>
 * <src.match.fail.del>
 * <scanner.cron>
 * <scanner>
 * <executor>
 * <executor.min>
 * <executor.max>
 * <executor.aliveTime.seconds>
 * <executor.overTime.seconds>
 * <executor.overTime.cancle>
 * <importer.batch>
 * 
 * @author X4584
 * @date 2018年12月12日
 *
 */
public class ImporterConfig extends Config {

	int batch;

	Configuration fsConf;

	protected ImporterConfig(String name) {
		super(name);
	}

	@Override
	void load() throws Exception {
		super.load();
		batch = XmlUtil.getInt(element, "importer.batch", 5000, 1, 50000);
		fsConf = new Configuration();
		fsConf.set("fs.defaultFS", "file:///");
	}
	
	@Override
	boolean valid() throws Exception {
		if(!super.valid()){
			return false;
		}
		//...
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("\nimporter.batch=" + batch);
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof ImporterConfig)){
			return false;
		}
		if(o == this){
			return true;
		}
		
		ImporterConfig c = (ImporterConfig)o; 
		if(!super.equals(c)){
			return false;
		}
		
		return batch == c.batch;
	}

	@Override
	public final String getType() {
		return TYPE_IMPORTER;
	}

	@Override
	public final String getTypeName() {
		return NAME_IMPORTER;
	}
}
