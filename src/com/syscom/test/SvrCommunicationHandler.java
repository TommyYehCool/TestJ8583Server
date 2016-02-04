package com.syscom.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvrCommunicationHandler extends Thread {
	
	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private final static String DISCONNECTED = "disconnected";
	
	private String mSessoinId;
	private boolean mConnected = false;
	
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	
	private MessageQ mMsgQ;
	private MessageHandler mMessageHandler;
	
	private ISO8583MsgHandler mISO8583MsgHandler;

	public SvrCommunicationHandler(String sessionId, Socket clnSocket) throws IOException {
		setName(this.getClass().getSimpleName());
		
		mSessoinId = sessionId;
		mConnected = true;
		
		mInputStream = clnSocket.getInputStream();
		mOutputStream = clnSocket.getOutputStream();
		
		mMsgQ = new MessageQ();
		mMessageHandler = new MessageHandler();
		mMessageHandler.start();
		
		mISO8583MsgHandler = ISO8583MsgHandler.getInstance();
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
				
				mISO8583MsgHandler.processISO8583(bClnMsg);
			}
			clientDisconnected();
		}
		catch (IOException e) {
			log.error("IOException raised while processing client request, terminate SvrCommunicationHandler thread, msg: <{}>", e.getMessage(), e);
		}
	}
	
	private boolean receiveFromSocket(byte[] msg) throws IOException {
		boolean disconnected = false;
		
		int totalMsgLen = msg.length;
		
		int toReadLen = totalMsgLen;
		
		int offset = 0;
		
		while (offset < totalMsgLen) {
			byte[] tmp = new byte[toReadLen];
			
			int readLen = mInputStream.read(tmp);

			disconnected = (readLen == -1);
			
			if (!disconnected) {
				System.arraycopy(tmp, 0, msg, offset, readLen);
				offset += readLen;
				toReadLen -= readLen;
			}
			else {
				break;
			}
		}
		return disconnected;
	}
	
	private void clientDisconnected() {
		log.warn("Detected client disconnected with sessionId: <{}>, "
				+ "1. Set connected flag to false, "
				+ "2. Terminate MessageHandler thread", mSessoinId);
		
		mConnected = false;
		sendMsgToClient(DISCONNECTED.getBytes());
	}
	
	public void sendMsgToClient(byte[] msg) {
		mMsgQ.offer(msg);
	}
	
	private class MessageHandler extends Thread {
		
		public MessageHandler() {
			setName(this.getClass().getSimpleName());
		}
		
		public void run() {
			try {
				while (mConnected) {
					List<byte[]> msgs = mMsgQ.getResps();
					
					int msgCounts = msgs.size();
					if (msgCounts == 1) {
						String msg = new String(msgs.get(0));
						if (msg.equals(DISCONNECTED)) {
							log.warn("Detected client disconnected, terminate MessageHandler thread...");
							break;
						}
					}
					
					for (int i = 0; i < msgCounts; i++) {
						byte[] bMsg = msgs.get(i);
						
						byte[] msgLen = ConvertUtil.convertIntTo2Bytes(bMsg.length);
						
						mOutputStream.write(msgLen);
						mOutputStream.write(bMsg);
						mOutputStream.flush();
						
						log.info("Send message to client done, msg: <{}>", new String(bMsg));
					}
				}
				log.warn("Received client disconnected signal, terminate MessageHandler thread");
			} 
			catch (InterruptedException e) {
				log.error("InterruptedException raised while getting response message from queue, MessageHandler thread terminated, msg: <{}>", e.getMessage(), e);
			} 
			catch (IOException e) {
				log.error("IOException raised while sending response message to client, MessageHandler thread terminated, msg: <{}>", e.getMessage(), e);
			}
		}
	}
}
