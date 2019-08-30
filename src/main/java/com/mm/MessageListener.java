package com.mm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mm.config.ApplicationConfigReader;

import atg.taglib.json.util.JSONException;
import atg.taglib.json.util.JSONObject;

/**
 * Message Listener for RabbitMQ
 * 
 * @author deepak.af.kumar
 *
 */

@Service
public class MessageListener {

	@Value("${json.path}")
	private String responseFilePath;

	private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

	private final RabbitTemplate rabbitTemplate;
	private ApplicationConfigReader applicationConfig;
	private MessageSender messageSender;

	public ApplicationConfigReader getApplicationConfig() {
		return applicationConfig;
	}

	@Autowired
	public void setApplicationConfig(ApplicationConfigReader applicationConfig) {
		this.applicationConfig = applicationConfig;
	}

	@Autowired
	public MessageListener(final RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public MessageSender getMessageSender() {
		return messageSender;
	}

	@Autowired
	public void setMessageSender(MessageSender messageSender) {
		this.messageSender = messageSender;
	}

	/**
	 * Message listener for app1
	 * 
	 * @param UserDetails
	 *            a user defined object used for deserialization of message
	 */
	@RabbitListener(queues = "${app1.queue.name}")
	public void receiveMessageForApp1(final Object data) {
		log.info("Received message: {} from app1 queue.", data);
		System.out.println("**************MESSAGE RECEIVED***************");
		System.out.println("DATA AS OBJECT " + data);
		String exchange = getApplicationConfig().getApp2Exchange();
		String routingKey = getApplicationConfig().getApp2RoutingKey();

		
		System.out.println("**************MESSAGE RECEIVED***************");

		try {
			String decodeData = this.decode(data.toString());
			System.out.println("*******decodeData********" + decodeData);
			atg.taglib.json.util.JSONObject json = new atg.taglib.json.util.JSONObject(decodeData);
			System.out.println(" decodeData JSON DATA " + json);
			
				
			
			if (json.has("Data")) {
				String strData = json.getString("Data");
				System.out.println("**********strData********"+strData);
				/*
				if (requestMetaData.has("Command")) {
					String command = requestMetaData.getString("Command");
					String commandSeqId = requestMetaData.getString("commandSeqId");
					if (command != null && commandSeqId != null) {
						command = command.toLowerCase().replace(" ", "");
						Object response = this.getRequestResponse(command, commandSeqId);
						messageSender.sendMessage(rabbitTemplate, exchange, routingKey, response.toString());
						log.info("<< Exiting receiveMessageForApp1() after API call.");
					}

				}
				*/
			}
		} catch (Exception e) {
			log.error("Internal server error occurred in API call. Bypassing message requeue {}", e);
			e.printStackTrace();
		}

	}

	public String decode(String str) {
		try {
			System.out.println(str);
			String data = URLDecoder.decode(str, "UTF-8").replace("(Body:", "");

			System.out.println(data);
			data = data.replace("\"Data\":\"{", "\"Data\":{");
			System.out.println(data);
			data = data.replace("}}\"}", "}}}");
			System.out.println(data);
			data = data.replace("\\", "");
			System.out.println(new JSONObject(data));
			return new JSONObject(data).toString();
		} catch (Exception e) {
			return "Issue while decoding" + e.getMessage();
		}
	}
	// {"Initial":"true","Data":{\"RequestMetaData\":{\"Command\":\"Search EandI\",\"requestId\":1567182814107,\"requestTime\":1567182814098,\"commandSeqId\":1},\"RequestParams\":{\"memberID\":\"\",\"FirstName\":\"\",\"LastName\":\"\",\"DOB\":null}}}' MessageProperties [headers={}, contentType=text/plain, contentLength=0, redelivered=false, receivedExchange=, receivedRoutingKey=request, deliveryTag=1, consumerTag=amq.ctag-K6-tA-bci2RhtKqhPwm7qA, consumerQueue=request])

	public static void main(String[] args) {
		String str = "";
		
		
		try {
			
			for (int i = 0; i < args.length; i++) {
				str = str + args[i];	
			}
			
			System.out.println(str);
			String data = URLDecoder.decode(str, "UTF-8").replace("(Body:", "");

			System.out.println(data);
			data = data.replace("\"Data\":\"{", "\"Data\":{");
			System.out.println(data);
			data = data.replace("}}\"}", "}}}");
			System.out.println(data);
			data = data.replace("\\", "");
			System.out.println(new JSONObject(data));
			
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Object getRequestResponse(String command, String commandSeqId) {
		String fileName = command + "_" + commandSeqId + ".json";

		String filePath = responseFilePath + fileName;
		System.out.println("Sending response from file ..." + filePath);
		StringBuilder content = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
			String line;
			while ((line = br.readLine()) != null) {
				content.append(line);
			}
			return new JSONObject(content.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return "";
	}

	/**
	 * Message listener for app2
	 * 
	 * Enable for testing purpose.
	 */
	@RabbitListener(queues = "${app2.queue.name}")
	public void receiveMessageForApp2(Object data) {
		log.info("Received message: {} from app2 queue.", data);
		System.out.println("**************MESSAGE RECEIVED***************");
		System.out.println("DATA AS OBJECT2 " + data);
		System.out.println("DATA AS TOSTRING2 " + data.toString());
		System.out.println("**************MESSAGE RECEIVED***************");
	}

}
