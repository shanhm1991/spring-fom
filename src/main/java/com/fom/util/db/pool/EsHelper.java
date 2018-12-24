package com.fom.util.db.pool;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.VersionConflictEngineException;

import com.fom.util.IoUtils;
import com.fom.util.log.LoggerFactory;

/**
 * 
 * @author shanhm
 * @date 2018年12月23日
 *
 */
public class EsHelper {

	protected static final Logger LOG = LoggerFactory.getLogger("es");

	protected EsHelper(){

	}

	protected final boolean _isIndexExist(String poolName, String index) throws Exception {
		IndicesExistsResponse response = PoolEs.getClient(poolName).admin().indices().
				exists(new IndicesExistsRequest().indices(new String[] { index })).actionGet();
		return response.isExists();
	}

	protected final void _createIndex(String poolName, String index) throws Exception{
		PoolEs.getClient(poolName).admin().indices().prepareCreate(index).execute().actionGet();
	}

	protected final void _delIndex(String poolName, String index) throws Exception{
		PoolEs.getClient(poolName).admin().indices().prepareDelete(index).execute().actionGet();
	}

	protected final void _mappingIndex(String poolName, String index, String type, File jsonFile) throws Exception {
		XContentParser parser = null;
		try{
			TransportClient client = PoolEs.getClient(poolName);
			parser = XContentFactory.xContent(XContentType.JSON).createParser(new FileInputStream(jsonFile));
			XContentBuilder builder = XContentFactory.jsonBuilder().copyCurrentStructure(parser);
			PutMappingRequest mappingRequest = Requests.putMappingRequest(index).type(type).source(builder);
			client.admin().indices().putMapping(mappingRequest).actionGet();
		}finally{
			IoUtils.close(parser);
		}
	}

	protected final List<Map<String,Object>> _multiGet(String poolName, String index, String type, Set<String> keySet) throws Exception {
		List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
		TransportClient client = PoolEs.getClient(poolName);

		MultiGetRequestBuilder muiltRequest = client.prepareMultiGet();
		for(String key : keySet){
			muiltRequest.add(index, type, key);
		}
		MultiGetResponse multiResps = muiltRequest.get();
		for(MultiGetItemResponse item : multiResps){
			GetResponse resp = item.getResponse();
			if(resp == null || !resp.isExists()){
				continue;
			}
			list.add(resp.getSourceAsMap());
		}
		return list;
	}

	protected final void _blukDelete(String poolName, String index, String type, Set<String> keySet) throws Exception {
		TransportClient client = PoolEs.getClient(poolName);
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for(String key : keySet){
			DeleteRequestBuilder delRequest = client.prepareDelete(index, type, key);
			bulkRequest.add(delRequest);
		}
		BulkResponse bulkResp = bulkRequest.execute().actionGet();
		if(bulkResp.hasFailures()){
			LOG.warn("删除失败数据：" + bulkResp.buildFailureMessage());
		}
	}

	protected Set<BulkItemResponse> _bulkUpdate(String poolName, String index, String type, Map<String,Map<String,Object>> data) throws Exception {
		Set<BulkItemResponse> conflictSet = new HashSet<>();
		TransportClient client = PoolEs.getClient(poolName);

		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for(Entry<String, Map<String, Object>> entry : data.entrySet()){
			UpdateRequestBuilder updateRequest = client.prepareUpdate(index, type, entry.getKey()).setDoc(entry.getValue());
			//清除 client.prepareDelete(config.getEsIndex(),config.getEsType(), entry.getKey())
			bulkRequest.add(updateRequest);
		}

		//VersionConflictEngineException可以处理掉
		BulkResponse bulkResp = bulkRequest.execute().actionGet();
		if(bulkResp.hasFailures()){
			BulkItemResponse[] items = bulkResp.getItems();
			for(BulkItemResponse item : items){
				if(item.getFailure().getCause() instanceof VersionConflictEngineException){
					conflictSet.add(item);
				}
				LOG.warn("更新失败数据：" + item.getFailureMessage());
			}
		}
		return conflictSet;
	}

	protected final void _bulkInsert(String poolName, String index, String type, Map<String,Map<String,Object>> data) throws Exception {
		TransportClient client = PoolEs.getClient(poolName);
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for(Entry<String, Map<String, Object>> entry : data.entrySet()){
			IndexRequestBuilder indexRequest = client.prepareIndex(index, type,entry.getKey()).setSource(entry.getValue());
			bulkRequest.add(indexRequest);
		}

		BulkResponse bulkResp = bulkRequest.execute().actionGet();
		if(bulkResp.hasFailures()){
			LOG.warn("新增失败数据：" + bulkResp.buildFailureMessage());
		}
	} 
}
