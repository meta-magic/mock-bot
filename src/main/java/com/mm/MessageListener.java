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
 * @author Ketan Gote
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
		log.info("*****************");
		log.info("Received message: {} from app1 queue.", data);		
		String exchange = getApplicationConfig().getApp2Exchange();
		String routingKey = getApplicationConfig().getApp2RoutingKey();

		

		try {
			String decodeData = this.decode(data.toString());
			JSONObject json = new JSONObject(decodeData);
			log.info("Decoded data {}.", decodeData);	
			
			if (json.has("Data")) {
				String strData = json.getString("Data");
				JSONObject requestMetaDataJson = new JSONObject(strData);
				JSONObject requestMetaDataJson1 = new JSONObject(requestMetaDataJson.getString("RequestMetaData"));
				
				if (requestMetaDataJson1.has("Command") && requestMetaDataJson1.has("commandSeqId")) {
					String command = requestMetaDataJson1.getString("Command");
					String commandSeqId = requestMetaDataJson1.getString("commandSeqId");
					log.info("Command {} and CommandSequenceId {} ",command,commandSeqId);
					
					if (command != null && commandSeqId != null) {
						
						command = command.toLowerCase().replace(" ", "");
						
						JSONObject response = this.getRequestResponse(command, commandSeqId);
						
						log.info("Sending Response {} For Command {} and CommandSequenceId ",response.toString() , command, commandSeqId);
						
						messageSender.sendMessage(rabbitTemplate, exchange, routingKey, response.toString());
						
						log.info("*******CHECKING IF FOR MULTIPLE RESPONSE PRESENT FOR THIS REQUEST**********");
						
						if(response.has("Data") && response.getJSONObject("Data").has("ResponseMetaData")){
						
							JSONObject newresponse =  response.getJSONObject("Data").getJSONObject("ResponseMetaData");
							Integer newcommandSeqId = new Integer(newresponse.getString("commandSeqId"));
							newcommandSeqId++;
							Integer newtotalResponsesAvailable = new Integer(newresponse.getString("totalResponsesAvailable"));
							
							log.info("Total Available Response {} ",newtotalResponsesAvailable);
							
							for (; newcommandSeqId <= newtotalResponsesAvailable; newcommandSeqId++) {
								
								log.info(" Command {} New CommandSequenceId ", command, newcommandSeqId);
								
								JSONObject nresponse = this.getRequestResponse(command, (newcommandSeqId)+"");
								Thread.sleep(2000);
								messageSender.sendMessage(rabbitTemplate, exchange, routingKey, nresponse.toString());
								
								log.info("Sending Response {} For Command {} and CommandSequenceId ",nresponse.toString(), command, newcommandSeqId);
								

							}
							
						}
						log.info("<< Exiting receiveMessageForApp1() after API call.");
					}

				}
				
			}
		} catch (Exception e) {
			log.error("Internal server error occurred in API call. Bypassing message requeue {}", e);
			e.printStackTrace();
		}
		log.info("*****************");
	}

	public String decode(String str) {
		try {
			String data = URLDecoder.decode(str, "UTF-8").replace("(Body:", "");

			data = data.replace("\"Data\":\"{", "\"Data\":{");
			data = data.replace("}}\"}", "}}}");
			data = data.replace("\\", "");
			data = data.replace(data.substring(data.indexOf("MessageProperties")),"");
			data = data.replace("'", "");
			return new JSONObject(data).toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "Issue while decoding" + e.getMessage();
		}
	}

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
			data = data.replace(data.substring(data.indexOf("MessageProperties")),"");
			System.out.println(data);
			System.out.println(new JSONObject(data));
			
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private JSONObject getRequestResponse(String command, String commandSeqId) {
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

		return new JSONObject();
	}

	/**
	 * Message listener for app2
	 * 
	 * Enable for testing purpose.
	
	@RabbitListener(queues = "${app2.queue.name}")
	public void receiveMessageForApp2(Object data) {
		log.info("Received message: {} from app2 queue.", data);
		System.out.println("**************MESSAGE RECEIVED***************");
		System.out.println("DATA AS OBJECT2 " + data);
		System.out.println("DATA AS TOSTRING2 " + data.toString());
		System.out.println("**************MESSAGE RECEIVED***************");
	}
 */
}
