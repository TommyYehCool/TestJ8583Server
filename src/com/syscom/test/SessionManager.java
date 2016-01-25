package com.syscom.test;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManager {

	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	public static SessionManager instance = new SessionManager();
	
	private Map<String, ClnCommunicationHandler> mClientConns = new HashMap<>();
	private Object mAccessConnsInfoLock = new Object();
	
	public static SessionManager getInstance() {
		return instance;
	}
	
	public void clientConnected(String sessionId, ClnCommunicationHandler clnCommunicationHandler) {
		synchronized (mAccessConnsInfoLock) {
			mClientConns.put(sessionId, clnCommunicationHandler);
		}
	}
	
	public void clientDisconnected(String disconnectSessionId) {
		synchronized (mAccessConnsInfoLock) {
			mClientConns.remove(disconnectSessionId);
		}
	}
	
	public void sendRespToClient(String sessionId, byte[] msg) {
		synchronized (mAccessConnsInfoLock) {
			ClnCommunicationHandler clnCommunicationHandler = mClientConns.get(sessionId);
			if (clnCommunicationHandler != null) {
				clnCommunicationHandler.sendRespToClient(msg);
			}
		}
	}
}
