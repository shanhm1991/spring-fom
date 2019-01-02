package com.fom.modules.importer.demo.local.es;

import java.util.List;
import java.util.Map;

import com.fom.context.Importer;

public class LocalEsPoolImpoter extends Importer<LocalEsImporterConfig, Map<String,Object>> {

	protected LocalEsPoolImpoter(String name, String path) {
		super(name, path);
	}

	@Override
	protected void praseLineData(LocalEsImporterConfig config, List<Map<String, Object>> lineDatas, String line,
			long batchTime) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void batchProcessLineData(LocalEsImporterConfig config, List<Map<String, Object>> lineDatas,
			long batchTime) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
