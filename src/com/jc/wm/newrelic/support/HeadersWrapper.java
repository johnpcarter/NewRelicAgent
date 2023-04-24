package com.jc.wm.newrelic.support;

import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.TransportType;

public interface HeadersWrapper extends Headers {

	 public TransportType getTransportType();
}
