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
public abstract class SDZipImporter<E extends SDZipImporterConfig,V> extends ZipImporter<E,V> {

	protected SDZipImporter(String name, String path) {
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
				lineDatas.add(praseLineData(config, line, batchTime));
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
