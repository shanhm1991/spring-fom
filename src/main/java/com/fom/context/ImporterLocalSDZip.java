package com.fom.context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;

import com.fiberhome.odin.hadoop.hdfs.io.SDFileReader;
import com.fom.util.IoUtils;

/**
 * 
 * @author shanhm1991
 *
 * @param <E>
 * @param <V>
 */
public abstract class ImporterLocalSDZip<E extends ImporterLocalSDZipConfig,V> extends ImporterLocalZip<E,V> {

	protected ImporterLocalSDZip(String name, String path) {
		super(name, path);
	}

	void readLine(File file, int StartLine) throws Exception {
		int lineIndex = 0;
		SDFileReader reader = null;
		String line = "";
		try{
			Path path = new Path(file.getPath());
			reader = new SDFileReader(path, config.fsConfig);
			List<V> lineDatas = null; 
			int batch = config.batch;
			if(batch > 0){
				lineDatas = new ArrayList<V>(batch);
			}else{
				lineDatas = new ArrayList<V>(2500);
			}

			long batchTime = System.currentTimeMillis();
			while ((line = reader.readStringLine()) != null) {
				lineIndex++;
				if(lineIndex <= StartLine){
					continue;
				}

				if(batch > 0 && lineDatas.size() >= batch){
					batchProcessIfNotInterrupted(lineDatas, batchTime); 
					updateLogFile(file.getName(), lineIndex);
					lineDatas.clear();
					batchTime = System.currentTimeMillis();
				}
				praseLineData(config, lineDatas, line, batchTime);
			}
			if(!lineDatas.isEmpty()){
				batchProcessIfNotInterrupted(lineDatas, batchTime); 
				updateLogFile(file.getName(), lineIndex);
			}
		}finally{
			IoUtils.close(reader);
		}
	}

}
