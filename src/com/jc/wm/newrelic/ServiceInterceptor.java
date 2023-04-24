package com.jc.wm.newrelic;

import java.net.URL;
import java.util.Iterator;

import com.wm.app.b2b.server.BaseService;
import com.wm.app.b2b.server.HTTPState;
import com.wm.app.b2b.server.InvokeState;
import com.wm.app.b2b.server.ProtocolInfoIf;
import com.wm.app.b2b.server.ProtocolState;
import com.wm.app.b2b.server.dispatcher.um.trigger.WmMessagingProtocolInfoImpl;
import com.wm.app.b2b.server.invoke.InvokeChainProcessor;
import com.wm.app.b2b.server.invoke.InvokeManager;
import com.wm.app.b2b.server.invoke.ServiceStatus;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataUtil;
import com.wm.util.ServerException;

import com.jc.wm.newrelic.support.HeadersWrapper;
import com.jc.wm.newrelic.support.InboundHttpWrapper;
import com.jc.wm.newrelic.support.JMSMesageWrapper;
import com.jc.wm.newrelic.support.OutboundHttpWrapper;
import com.jc.wm.newrelic.support.PublishMesageWrapper;

import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;

public class ServiceInterceptor implements InvokeChainProcessor {

	public static ServiceInterceptor defaultInstance;

	public static void register() {
    	
		if (defaultInstance == null) {
			defaultInstance = new ServiceInterceptor();
		}
		
	    InvokeManager.getDefault().registerProcessor(defaultInstance);
	}
	    
	public static void unregister() {
		InvokeManager.getDefault().unregisterProcessor(defaultInstance);
	}
	
	public ServiceInterceptor() {
	}
	
	@Trace(dispatcher = true)
	@Override
	public void process(Iterator chain, BaseService svc, IData pipeline, ServiceStatus status) throws ServerException {
		
		String serviceName = getServiceName(svc);
		boolean isTopLevelService = status.isTopService();
		
		try {
			
			Segment s = null;
			
			if (NewRelic.getAgent().getTransaction() != null) {
				
				if (isTopLevelService) {
					
					System.out.println(">>>>>>>> Recording Transaction " + serviceName);
					
					this.continueTransaction(pipeline);
					
					if (serviceName.equals("service.apigateway:gatewayClient")) {
						
						// record URL and not service name
						
						ProtocolState state = (ProtocolState) getTransportInfo();
						
						serviceName = new URL(state.getRequestUrl()).getPath();
					}
					
					NewRelic.setTransactionName("service", serviceName);
					
				} else if (serviceName.equals("pub.client:http")) {
					this.propagateHttpTransaction(pipeline);
				} else if (serviceName.equals("pub.publish:publish")) {
					this.propagateMessagingTransaction(pipeline);
				} else if (serviceName.equals("pub.jms:send")) {
					this.propagateJMSTransaction(pipeline);
				} else {
					
					System.out.println(">>>>>>>> Recording Segment " + serviceName);

					s = NewRelic.getAgent().getTransaction().startSegment(serviceName);
				}
				
			} else {
				System.out.println(">>>>>>>>>>>>>>>>> No transaction recorded");
			}
			
			if(chain.hasNext()) {
				
				((InvokeChainProcessor) chain.next()).process(chain, svc, pipeline, status);
			
				if (isTopLevelService) {
					String cid = InvokeState.getCurrentState().getCustomAuditContextID();
					
					if (cid != null) {
						NewRelic.addCustomParameter("customContextId", cid);
					}
					
					System.out.println("<<<<<<< Ending Transaction " + serviceName);

					NewRelic.recordResponseTimeMetric("execution", System.currentTimeMillis() - status.getStartTime());
				} else if (s != null) {
					
					System.out.println("<<<<<<< Ending Segment " + serviceName);

					s.end();
				}
			 }
		} catch (Exception e) {
			
			NewRelic.noticeError(e);
			
			throw e;
		}

	}

	protected String getServiceName(BaseService baseService) {
		return baseService.getNSName().getFullName();
	}
	
	protected String[] getContextIDsForService() {
        
		String[] contextIDs = {null, null, null};

	    try {
	    	InvokeState currentInvokeState = InvokeState.getCurrentState();
	        //Stack<?> servicesStack = currentInvokeState.getCallStack();
	        String contextIDStack[] = currentInvokeState.getAuditRuntime().getContextStack();

	        String contextId = null;
	        String parentContextId = null;
	        String rootContextId = null;

	        int contextId_index = contextIDStack.length - 1;


	        contextId = contextIDStack[contextId_index];
	        if (contextId_index > 0) {
	        	parentContextId = contextIDStack[contextId_index - 1];
	        }
	            
	        rootContextId = contextIDStack[0];

	        contextIDs[0] = contextId;
	        contextIDs[1] = parentContextId;
	        contextIDs[2] = rootContextId;
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        }

	        return contextIDs;
	}
	
	private void continueTransaction(IData pipeline) {
		
		ProtocolInfoIf state = getTransportInfo();
		
		if (state instanceof HTTPState) {
			this.continueHttpTransaction((ProtocolState) state);
		} else if (state instanceof WmMessagingProtocolInfoImpl) {
			
			if (((WmMessagingProtocolInfoImpl) state).getSubProtocol().equalsIgnoreCase("broker")) {
				this.continueMessagingTransaction(pipeline);
			} else {
				this.continueJMSTransaction(pipeline);
			}
		}
	}
	
	private void continueHttpTransaction(ProtocolState state) {
		
		HeadersWrapper req = new InboundHttpWrapper(state);
    	
	    // Set the request for the current transaction and convert it into a web transaction
    	NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(req.getTransportType(), req);
	}
	
	private void continueMessagingTransaction(IData pipeline) {
		
	// Wrap the outbound Request object
		
		IDataCursor c = pipeline.getCursor();
		IData message = null;
		c.first();
		do {
			if (c.getValue() instanceof IData) {
				message = (IData) c.getValue();
			}
					
			c.next();	
		} while (c.hasMoreData() && message == null);
					
		c.destroy();
				
		if (message != null) {
			HeadersWrapper outboundHeaders = new PublishMesageWrapper(message);
	    	NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(outboundHeaders.getTransportType(), outboundHeaders);
		}
	}
	
	private void continueJMSTransaction(IData pipeline) {
		
	// Wrap the outbound Request object
			
		IDataCursor c = pipeline.getCursor();
		Object messages = IDataUtil.get(c, "JMSMessage");		
		c.destroy();
		
		IData message = null;
		
		if (messages instanceof IData) {
			message = (IData) messages;
		} else if (messages instanceof IData[] && ((IData[]) messages).length > 0) {
			message = ((IData[]) messages)[0];
		}
					
		if (message != null) {
			HeadersWrapper outboundHeaders = new JMSMesageWrapper(message);
	    	NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(outboundHeaders.getTransportType(), outboundHeaders);
		}
	}
		
	private void propagateHttpTransaction(IData pipeline) {
		
		 // Wrap the outbound Request object
	    Headers outboundHeaders = new OutboundHttpWrapper(pipeline);

	    // Obtain a reference to the current transaction
	    Transaction transaction = NewRelic.getAgent().getTransaction();
	    // Add headers for outbound external request
	    transaction.insertDistributedTraceHeaders(outboundHeaders);
	}
	
	private void propagateMessagingTransaction(IData pipeline) {
		
		// Wrap the outbound Request object
		
		IDataCursor c = pipeline.getCursor();
		IData message = IDataUtil.getIData(c, "document");
		c.destroy();
		
		if (message != null) {
			Headers outboundHeaders = new PublishMesageWrapper(message);

	    	// Obtain a reference to the current transaction
	    	Transaction transaction = NewRelic.getAgent().getTransaction();
	    	// Add headers for outbound external request
	    	transaction.insertDistributedTraceHeaders(outboundHeaders);
		}
	}
	
	private void propagateJMSTransaction(IData pipeline) {
		
		IDataCursor c = pipeline.getCursor();
		IData message = IDataUtil.getIData(c, "JMSMessage");
		c.destroy();
		
	    Headers outboundHeaders = new JMSMesageWrapper(message);

	    // Obtain a reference to the current transaction
	    Transaction transaction = NewRelic.getAgent().getTransaction();
	    // Add headers for outbound external request
	    transaction.insertDistributedTraceHeaders(outboundHeaders);
	}
		
	public static final ProtocolInfoIf getTransportInfo() {

		ProtocolInfoIf protocolInfo;
		InvokeState is = InvokeState.getCurrentState();
		        
		if (is != null && (protocolInfo = is.getProtocolInfoIf()) != null) {
		     
			return protocolInfo;
			/*if (protocolInfo instanceof ProtocolState) {
		      	return (ProtocolState) protocolInfo;
		    } else {
		    	return null;
		    }*/
		} else {
			return null;
		}
	}
}
