package com.jc.wm.newrelic.support;

import java.util.Collection;
import java.util.HashSet;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;

public class OutboundHttpWrapper implements Headers {

	private static String HEADERS = "headers";
	
	private IData headers = null;
	
	public OutboundHttpWrapper(IData pipeline) {
		
		IDataCursor c = pipeline.getCursor();
		this.headers = IDataUtil.getIData(c, HEADERS);
		
		if (this.headers == null) {
			this.headers = IDataFactory.create();
			IDataUtil.put(c, HEADERS, this.headers);
		}
		
		c.destroy();
	}
	
	@Override
	public HeaderType getHeaderType() {
	    return HeaderType.HTTP;
	}
	
	@Override
	public void setHeader(String key, String value) {
				
		IDataCursor c = headers.getCursor();
		IDataUtil.put(c, key, value);
		c.destroy();

	}
	
	@Override
	public void addHeader(String key, String value) {
		
		IDataCursor c = headers.getCursor();
		c.insertAfter(key, value);
		c.destroy();
	}

	@Override
	public boolean containsHeader(String key) {
		
		return this.getHeader(key) != null;
	}

	@Override
	public String getHeader(String key) {
		
		IDataCursor c = headers.getCursor();
		String value = IDataUtil.getString(c, key);
		c.destroy();
		
		return value;
	}

	@Override
	public Collection<String> getHeaderNames() {
		
		HashSet<String> out = new HashSet<String>();
		
		IDataCursor c = headers.getCursor();
		
		c.first();
		while(c.hasMoreData()) {
		
			out.add(c.getKey());
			c.next();
		}
		
		c.destroy();
		
		return out;
	}

	@Override
	public Collection<String> getHeaders(String key) {

		HashSet<String> out = new HashSet<String>();
		
		IDataCursor c = headers.getCursor();
		
		c.first();
		while(c.hasMoreData()) {
		
			if (c.getKey().equalsIgnoreCase(key)) {
				out.add((String) c.getValue());
			}
			
			c.next();
		}
		
		c.destroy();
		
		return out;
	}
}
