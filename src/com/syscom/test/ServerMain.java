package com.syscom.test;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;

public class ServerMain {
	
	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private int mPort = 1234;
	
	private MessageFactory<IsoMessage> mf = new MessageFactory<IsoMessage>();
	
	private void start() {
		loadLog4jConfig();
		
		loadJ8583Config();

		startServer();
	}

	private void loadLog4jConfig() {
		String log4jConfig = "./config/log4j.properties";

		PropertyConfigurator.configure(log4jConfig);
		
		log.info("Load log4j config succeed, path: [{}]", log4jConfig);
	}

	private void loadJ8583Config() {
		String j8583Config = "./config/j8583-config.xml";
		
		try {
			ConfigParser.configureFromUrl(mf, new File(j8583Config).toURI().toURL());
			
			log.info("Load J8583 config succeed, path: [{}]", j8583Config);
		} 
		catch (IOException e) {
			log.error("IOException raised while loading J8583 config, msg: <{}>", e.toString(), e);
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
				
				log.info("Client connected, info: <{}>", sessionId);
				
				ClnCommunicationHandler clnCommunicationHandler 
					= new ClnCommunicationHandler(sessionId, clnSocket);
				
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
