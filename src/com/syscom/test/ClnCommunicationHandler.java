package com.syscom.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClnCommunicationHandler extends Thread {
	
	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private final static String DISCONNECTED = "disconnected";
	
	private String mSessoinId;
	private boolean mConnected = false;
	
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	
	private RespForClnQ mRespForClnQ;
	private RespForClnHandler mRespForClnHandler;

	public ClnCommunicationHandler(String sessionId, Socket clnSocket) throws IOException {
		mSessoinId = sessionId;
		mConnected = true;
		
		mInputStream = clnSocket.getInputStream();
		mOutputStream = clnSocket.getOutputStream();
		
		mRespForClnQ = new RespForClnQ();
		mRespForClnHandler = new RespForClnHandler();
		mRespForClnHandler.start();
	}
	
	public void run() {
		try {
			while (mConnected) {
				byte[] bMsgLen = new byte[2];
				
				boolean isClientDisconnected = receiveFromSocket(bMsgLen);
				if (isClientDisconnected) {
					break;
				}
				
				int msgLen = ConvertUtil.convert2BytesToInt(bMsgLen);
				log.info("Received client message, length: <{}>", msgLen);
				
				byte[] bClnMsg = new byte[msgLen];
				isClientDisconnected = receiveFromSocket(bClnMsg);
				
				if (isClientDisconnected) {
					break;
				}
				
				log.info("Received client message done, msg: <{}>", new String(bClnMsg));
				
				MessageHandler.processISO8583(bClnMsg);
			}
			clientDisconnected();
		}
		catch (IOException e) {
			log.error("IOException raised while processing client request, terminate ClnCommunicationHandler thread, msg: <{}>", e.getMessage(), e);
		}
	}
	
	private boolean receiveFromSocket(byte[] msg) throws IOException {
		boolean isClientDisconnected = false;
		
		int totalMsgLen = msg.length;
		
		int toReadLen = totalMsgLen;
		
		int offset = 0;
		
		while (offset < totalMsgLen) {
			byte[] tmp = new byte[toReadLen];
			
			int readLen = mInputStream.read(tmp);

			isClientDisconnected = (readLen == -1);
			
			if (!isClientDisconnected) {
				System.arraycopy(tmp, 0, msg, offset, readLen);
				offset += readLen;
				toReadLen -= readLen;
			}
			else {
				break;
			}
		}
		return isClientDisconnected;
	}
	
	private void clientDisconnected() {
		log.warn("Detected client disconnected with sessionId: <{}>, "
				+ "1. Set ClnCommuncationHandler connected flag to false, "
				+ "2. Terminate RespForClnHandler thread", mSessoinId);
		
		mConnected = false;
		sendRespToClient(DISCONNECTED.getBytes());
	}
	
	public void sendRespToClient(byte[] msg) {
		mRespForClnQ.offer(msg);
	}
	
	private class RespForClnHandler extends Thread {
		public void run() {
			try {
				while (mConnected) {
					List<byte[]> respMsg = mRespForClnQ.getResps();
					
					int msgCounts = respMsg.size();
					if (msgCounts == 1) {
						String msg = new String(respMsg.get(0));
						if (msg.equals(DISCONNECTED)) {
							log.warn("Detected client disconnected, terminate RespForClnHandler thread...");
							break;
						}
					}
					
					for (int i = 0; i < msgCounts; i++) {
						byte[] bRespMsgs = respMsg.get(i);
						
						byte[] msgLen = ConvertUtil.convertIntTo2Bytes(bRespMsgs.length);
						
						mOutputStream.write(msgLen);
						mOutputStream.write(bRespMsgs);
						mOutputStream.flush();
						
						log.info("Send response message to client done, msg: <{}>", new String(bRespMsgs));
					}
				}
				log.warn("Received client disconnected signal, terminate RespForClnHandler thread");
			} 
			catch (InterruptedException e) {
				log.error("InterruptedException raised while getting response message from queue, RespForClnHandler terminated, msg: <{}>", e.getMessage(), e);
			} 
			catch (IOException e) {
				log.error("IOException raised while sending response message to client, RespForClnHandler terminated, msg: <{}>", e.getMessage(), e);
			}
		}
	}
}
