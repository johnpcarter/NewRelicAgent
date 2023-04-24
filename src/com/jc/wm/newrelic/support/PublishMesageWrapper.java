package com.jc.wm.newrelic.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.TransportType;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;

public class PublishMesageWrapper implements HeadersWrapper {

	private static final String HEADERS = "_env";
	private static final String HEADERS_ATTRIB = "transactionId";

	private static final String FIELD_SEPARATOR = "?";
	private static final String KEY_SEPARATOR = ":";
	
	private IData headersWrapper = null;
	
	public PublishMesageWrapper(IData message) {
		
		IDataCursor c = message.getCursor();
		this.headersWrapper = IDataUtil.getIData(c, HEADERS);
		
		if (this.headersWrapper == null) {
			this.headersWrapper = IDataFactory.create();
			IDataUtil.put(c, HEADERS, this.headersWrapper);
		}
		
		c.destroy();
	}
	
	public TransportType getTransportType() {
	 	return TransportType.Other;
	}
	
	@Override
	public HeaderType getHeaderType() {
	    return HeaderType.MESSAGE;
	}
	
	@Override
	public void setHeader(String key, String value) {
		
		String transactionId = this.getTransactionId();
		
		if (transactionId != null && transactionId.contains(key.toLowerCase())) {
			// update existing value
			
			int start = transactionId.lastIndexOf(key) + key.length() + 1;
			int end = transactionId.substring(start).indexOf(FIELD_SEPARATOR);
			
			String frag = transactionId.substring(0, start) + KEY_SEPARATOR + value;

			if (end != -1) {
				frag += transactionId.substring(start+end);
			}
			
			transactionId = frag;
			
		} else if (transactionId != null && transactionId.length() > 0) {
			transactionId += FIELD_SEPARATOR + key.toLowerCase() + KEY_SEPARATOR + value;
		} else {
			transactionId = key.toLowerCase() + KEY_SEPARATOR + value;
		}
		
		this.setTransactionId(transactionId);
	}
	
	@Override
	public void addHeader(String key, String value) {
		
		String transactionId = this.getTransactionId();
		
		
		if (transactionId.length() > 0) {
			transactionId += FIELD_SEPARATOR + key.toLowerCase() + KEY_SEPARATOR + value;
		} else {
			transactionId = key.toLowerCase() + KEY_SEPARATOR + value;
		}
				
		this.setTransactionId(transactionId);
	}

	@Override
	public boolean containsHeader(String key) {
		
		System.out.println("looking for key " + key);
		
		return this.getHeader(key) != null;
	}

	@Override
	public String getHeader(String key) {
		
		List<String> values = this.getHeaders().get(key);
		
		System.out.println("getting key " + key);

		if (values.size() > 0) {
			
			System.out.println("<<<< found " + key + " = " + values.get(0));
			return values.get(0);
		} else {
			return null;
		}
	}

	@Override
	public Collection<String> getHeaderNames() {
		
		return this.getHeaders().keySet();
	}

	@Override
	public Collection<String> getHeaders(String key) {

		List<String> hdrs = new ArrayList<String>();
		Map<String, List<String>>allHeaders = this.getHeaders();
		
		System.out.println("---- getting key " + key);

		for (String k : allHeaders.keySet()) {
			
			if (k.equalsIgnoreCase(key)) {
				hdrs = allHeaders.get(k);
			}
		}
		
		return hdrs;
	}
	
	private Map<String, List<String>> getHeaders() {
	
		HashMap<String, List<String>> hdrs = new HashMap<String, List<String>>();

		String transactionId = this.getTransactionId();
		
		if (transactionId != null && transactionId.contains(FIELD_SEPARATOR)) {
			StringTokenizer keyValues = new StringTokenizer(transactionId, FIELD_SEPARATOR);
			
			while(keyValues.hasMoreElements()) {
				
				makeValue(keyValues.nextToken(), hdrs);
			}
		} else if (transactionId != null) {
			
			makeValue(transactionId, hdrs);
		}
		
		return hdrs;
	}
	
	private String makeValue(String transactionId, HashMap<String, List<String>> hdrs) {
		
		StringTokenizer kvt = new StringTokenizer(transactionId, KEY_SEPARATOR);
		String key = kvt.nextToken();
		String value = kvt.nextToken();
		
		if (hdrs.containsKey(key)) {
			ArrayList<String> values = (ArrayList<String>) hdrs.get(key);
			values.add(value);
		} else {
			ArrayList<String> values = new ArrayList<String>();
			values.add(value);
			hdrs.put(key, values);
		}
		
		return key;
	}
	
	private String getTransactionId() {
		
		IDataCursor c = headersWrapper.getCursor();
		String transactionId = IDataUtil.getString(c, HEADERS_ATTRIB);
		c.destroy();
		
		return transactionId;
	}
	
	private void setTransactionId(String id) {
		
		IDataCursor c = headersWrapper.getCursor();
		IDataUtil.put(c, HEADERS_ATTRIB, id);
		c.destroy();
	}
}
