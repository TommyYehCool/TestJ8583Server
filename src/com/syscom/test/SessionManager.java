package com.syscom.test;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {

	public static SessionManager instance = new SessionManager();
	
	private Map<String, SvrCommunicationHandler> mClientConns = new HashMap<>();
	private Object mAccessConnsInfoLock = new Object();
	
	public static SessionManager getInstance() {
		return instance;
	}
	
	public void clientConnected(String sessionId, SvrCommunicationHandler clnCommunicationHandler) {
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
			SvrCommunicationHandler clnCommunicationHandler = mClientConns.get(sessionId);
			if (clnCommunicationHandler != null) {
				clnCommunicationHandler.sendMsgToClient(msg);
			}
		}
	}
}
