package com.jc.wm.newrelic.support;

import java.util.ArrayList;
import java.util.Collection;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.TransportType;
import com.wm.app.b2b.server.InvokeState;
import com.wm.app.b2b.server.ProtocolInfoIf;
import com.wm.app.b2b.server.ProtocolState;

public class InboundHttpWrapper implements HeadersWrapper {
	
    public static final String TRANSPORT_DOC_NAME = "transport";

    private final ProtocolState state;

    public InboundHttpWrapper(ProtocolState httpState) {
    	
    	if (httpState.getProtocol().contains("http")) {
    		this.state = httpState;
    	} else {
    		throw new InvalidProtocolState();
    	}
    }

    public TransportType getTransportType() {
    	return TransportType.HTTP;
    }
    
    @Override
    public HeaderType getHeaderType() {
    	return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
    	if (this.state != null) {
    		String value = this.state.getRequestHeader().getFieldValue(name);
    		
    		if (value != null) {
    			System.out.println(">>>>>> got " + name + " = " + value);
    		}
    		return value;
    	} else {
    		return null;
    	}
    }

    @Override
    public Collection<String> getHeaders(String name) {
    	
    	if (this.state != null) {
    		ArrayList<String> headers = new ArrayList<String>();
    		
    		headers.add(this.state.getRequestHeader().getFieldValue(name));
    		
    		return headers;
    	} else {
    		return null;
    	}
    }

    @Override
    public Collection<String> getHeaderNames() {
    	
    	if (this.state != null) {
    		return this.state.getRequestHeader().getFieldsMap().keySet();
    	} else {
    		return null;
    	}
    }

    @Override
    public boolean containsHeader(String name) {
       
    	if (this.state != null) {
    		return this.state.getRequestHeader().getFieldsMap().containsKey(name);
    	} else {
    		return false;
    	}
    }
    
    @Override
    public void setHeader(String name, String value) {
    	
    	throw new NotImplementedException();
    }

    
    @Override
    public void addHeader(String name, String value) {
    	throw new NotImplementedException();
    }
    
    class NotImplementedException extends RuntimeException {

		private static final long serialVersionUID = 8658127630130540965L;
    	
    }
    
    class InvalidProtocolState extends RuntimeException {

		private static final long serialVersionUID = -140038913207140519L;
    	
    }
}
