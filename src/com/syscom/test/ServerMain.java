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

public class ServerMain {
	
	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private int mPort = 1234;
	
	private void start() {
		loadLog4jConfig();
		
		showPid();
		
		initMessageHandler();

		startServer();
	}

	private void loadLog4jConfig() {
		String log4jConfig = "./config/log4j.properties";

		PropertyConfigurator.configure(log4jConfig);
		
		log.info("Load log4j config succeed, path: <{}>", log4jConfig);
	}
	
	private void showPid() {
		log.info("Process ID: <{}>", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	}

	private void initMessageHandler() {
		boolean initSucceed = SvrMessageHandler.getInstance().init();
		if (initSucceed) {
			log.info("Initialize MessageHandler done");
		}
		else {
			log.error("Initialize MessageHandler failed");
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
				
				SvrCommunicationHandler clnCommunicationHandler 
					= new SvrCommunicationHandler(sessionId, clnSocket);
				
				clnCommunicationHandler.start();
				
				SessionManager.getInstance().clientConnected(sessionId, clnCommunicationHandler);
			}
		}
		catch (IOException e) {
			log.error("IOException raised while waiting client to connect or starting ClnCommunicationHandler, msg: <{}>", e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		new ServerMain().start();
	}
}
