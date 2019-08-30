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
     * @param UserDetails a user defined object used for deserialization of message
     */
    @RabbitListener(queues = "${app1.queue.name}")
    public void receiveMessageForApp1(final Object data) {
    	log.info("Received message: {} from app1 queue.", data);
    	System.out.println("**************MESSAGE RECEIVED***************");
    	System.out.println("DATA AS OBJECT "+data);
    	//System.out.println("DATA AS TOSTRING "+data.getParameter("Data"));
    	
		String exchange = getApplicationConfig().getApp2Exchange();
		String routingKey = getApplicationConfig().getApp2RoutingKey();

		
		try {
			String decodeData = this.decode(data.toString());
			System.out.println("*******decodeData********"+decodeData);
			atg.taglib.json.util.JSONObject json = new atg.taglib.json.util.JSONObject(decodeData);
			System.out.println("JSON DATA "+json);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("**************MESSAGE RECEIVED***************");
		
		
//    	try {
//    		log.info("Getting Response Associated to Request");
//    		atg.taglib.json.util.JSONObject json = new atg.taglib.json.util.JSONObject(data);
//    		if(json.has("Data") && json.getJSONObject("Data").has("RequestMetaData")){
//    			JSONObject requestMetaData = json.getJSONObject("Data").getJSONObject("RequestMetaData");
//    			if(requestMetaData.has("Command")){
//    				String command = requestMetaData.getString("Command");
//    				String commandSeqId = requestMetaData.getString("commandSeqId");
//    				if (command !=null && commandSeqId!=null){
//    					command = command.toLowerCase().replace(" ", "");
//    					Object response = this.getRequestResponse(command, commandSeqId);
//    		    		messageSender.sendMessage(rabbitTemplate, exchange, routingKey, response.toString());
//    		        	log.info("<< Exiting receiveMessageForApp1() after API call.");
//    				}
//    				
//    			}
//    		}
//    	}  catch(Exception e) {
//    		log.error("Internal server error occurred in API call. Bypassing message requeue {}", e);
//    		throw new AmqpRejectAndDontRequeueException(e); 
//    	}

    }
    
    public  String decode(String url)  
    {  
              try {  
                   String prevURL="";  
                   String decodeURL=url;  
                   while(!prevURL.equals(decodeURL))  
                   {  
                        prevURL=decodeURL;  
                        decodeURL=URLDecoder.decode( decodeURL, "UTF-8" );  
                   }  
                   return decodeURL;  
              } catch (UnsupportedEncodingException e) {  
                   return "Issue while decoding" +e.getMessage();  
              }  
    }  
	
    
    private Object getRequestResponse(String command, String commandSeqId){
    	String fileName = command+"_"+commandSeqId+".json";
    	
    	String filePath = responseFilePath+fileName;
    	System.out.println("Sending response from file ..."+filePath);
    	StringBuilder content = new StringBuilder(); 
    	try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
			String line ;
			while((line = br.readLine())!=null){
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
      Enable for testing purpose.
    */
	    @RabbitListener(queues = "${app2.queue.name}")
	    public void receiveMessageForApp2(Object data) {
	    	log.info("Received message: {} from app2 queue.", data);
	    	System.out.println("**************MESSAGE RECEIVED***************");
	    	System.out.println("DATA AS OBJECT2 "+data);
	    	System.out.println("DATA AS TOSTRING2 "+data.toString());
	    	System.out.println("**************MESSAGE RECEIVED***************");
	    }

     
}
