package com.jc.wm.newrelic.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.TransportType;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataUtil;

public class JMSMesageWrapper implements HeadersWrapper {

	private static final String PROPERTIES = "properties";

	private IData properties = null;
	
	public JMSMesageWrapper(IData message) {
		
		IDataCursor c = message.getCursor();
		this.properties = IDataUtil.getIData(c, PROPERTIES);
		c.destroy();
		
	}
	
	public TransportType getTransportType() {
	 	return TransportType.JMS;
	}
	
	@Override
	public HeaderType getHeaderType() {
	    return HeaderType.MESSAGE;
	}
	
	@Override
	public void setHeader(String key, String value) {
		
		IDataCursor c = this.properties.getCursor();
		IDataUtil.put(c, key, value);
		c.destroy();
	}
	
	@Override
	public void addHeader(String key, String value) {
		
		IDataCursor c = this.properties.getCursor();
		c.first();
		c.insertAfter(key, value);
		c.destroy();
	}

	@Override
	public boolean containsHeader(String key) {
		
		return this.getHeader(key) != null;
	}

	@Override
	public String getHeader(String key) {
		
		IDataCursor c = this.properties.getCursor();
		String value = IDataUtil.getString(c, key);
		c.destroy();
		
		return value;
	}

	@Override
	public Collection<String> getHeaderNames() {
		
		HashSet<String> hdrs = new HashSet<String>();
		
		IDataCursor c =  this.properties.getCursor();
		c.first();
		while(c.hasMoreData()) {
			hdrs.add(c.getKey());	
			c.next();
		}
		
		return hdrs;
	}

	@Override
	public Collection<String> getHeaders(String key) {

		List<String> hdrs = new ArrayList<String>();
		
		IDataCursor c =  this.properties.getCursor();
		c.first();
		while(c.hasMoreData()) {
			
			if (c.getKey().equalsIgnoreCase(key)) {
				hdrs.add((String) c.getValue());
			}
			
			c.next();
		}
		
		return hdrs;
	}
}
