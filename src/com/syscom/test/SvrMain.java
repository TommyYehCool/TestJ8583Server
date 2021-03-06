package com.syscom.test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvrMain {
	
	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private int mPort = 1234;
	
	private void start() {
		loadLog4jConfig();
		
		showInitLog();
		
		initISO8583MsgHandler();

		startServer();
	}

	private void loadLog4jConfig() {
		String log4jConfig = "./config/log4j.properties";

		PropertyConfigurator.configure(log4jConfig);
		
		log.info("Load log4j config succeed, path: <{}>", log4jConfig);
	}
	
	private void showInitLog() {
		log.info("------- Try to init client process with process ID: <{}> -------", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	}

	private void initISO8583MsgHandler() {
		boolean initSucceed = ISO8583MsgHandler.getInstance().init();
		if (initSucceed) {
			log.info("Initialize ISO8583MsgHandler done");
		}
		else {
			log.error("Initialize ISO8583MsgHandler failed");
			System.exit(1);
		}
	}

	private void startServer() {
		ServerSocket svrSocket = null;
		try {
			svrSocket = new ServerSocket(mPort);
			log.info("Create server socket with port: <{}> succeed", mPort);
		} catch (IOException e) {
			log.error("IOException raised while creating server socket, msg: <{}>", e.getMessage(), e);
			System.exit(1);
		}
		
		InetAddress myComputer;
		try {
			myComputer = InetAddress.getLocalHost();
			log.info("Waiting for client to connect at IP: <{}>, port: <{}>", myComputer.getHostAddress(), mPort);
		} 
		catch (UnknownHostException e) {
			log.error("Exception raised while getting local IP by InetAddress, msg: <{}>", e.getMessage());
			System.exit(1);
		}
		
		try {
			while (true) {
				Socket clnSocket = svrSocket.accept();
				clnSocket.setTcpNoDelay(true);
				
				String sessionId = clnSocket.getInetAddress().toString().substring(1) + ":" + clnSocket.getPort();
				
				log.info("Client connected, sessionId: <{}>", sessionId);
				
				SvrCommunicationHandler communicationHandler 
					= new SvrCommunicationHandler(sessionId, clnSocket);
				
				communicationHandler.start();
				
				log.info("Initialize SvrCommunicationHandler done");
				
				SessionManager.getInstance().clientConnected(sessionId, communicationHandler);
				
				log.info("------- Prepare to communicate with client -------");
			}
		}
		catch (IOException e) {
			log.error("IOException raised while waiting client to connect or starting ClnCommunicationHandler, msg: <{}>", e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		new SvrMain().start();
	}
}
