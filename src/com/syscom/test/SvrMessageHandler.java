package com.syscom.test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;

public class SvrMessageHandler {
	
	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private static SvrMessageHandler instance = new SvrMessageHandler();
	
	private MessageFactory<IsoMessage> mf = new MessageFactory<IsoMessage>();
	
	public static SvrMessageHandler getInstance() {
		return instance;
	}
	
	public boolean init() {
		boolean succeed = true;
		
		String j8583Config = "./config/j8583-config.xml";
		try {
			ConfigParser.configureFromUrl(mf, new File(j8583Config).toURI().toURL());
			
			log.info("Load J8583 config succeed, path: <{}>", j8583Config);
		} 
		catch (IOException e) {
			log.error("IOException raised while loading J8583 config, msg: <{}>", e.toString(), e);
			
			succeed = false;
		}
		return succeed;
	}
	
	public void processISO8583(byte[] bClnMsg) {
		try {
			IsoMessage isoMsg = mf.parseMessage(bClnMsg, 0);
			if (isoMsg != null) {
				System.out.printf("\nMessage type: %04x%n", isoMsg.getType());
				System.out.println("FIELD TYPE    VALUE");
				for (int i = 2; i <= 128; i++) {
					IsoValue<?> f = isoMsg.getField(i);
					if (f != null) {
						System.out.printf("%5d %-6s [", i, f.getType());
						System.out.print(f.toString());
						System.out.println(']');
					}
				}
			}
			else {
				log.warn("Parse client ISO8583 message is null");
			}
		} 
		catch (UnsupportedEncodingException e) {
			log.warn("UnsupportedEncodingException raised while parsing client ISO8583 message, msg: <{}>", e.getMessage(), e);
		} 
		catch (ParseException e) {
			log.warn("ParseException raised while parsing client ISO8583 message, msg: <{}>", e.getMessage(), e);
		}
	}
}
