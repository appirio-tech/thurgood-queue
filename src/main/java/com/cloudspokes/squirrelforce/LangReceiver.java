package com.cloudspokes.squirrelforce;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

public class LangReceiver implements Runnable {
	private String lang = null;
	private Connection connection = null;

	public LangReceiver(String lang, Connection connection) {
		this.lang = lang;
		this.connection = connection;
	}
	
	public void run() {
		Channel receiveChannel = null;

		try {
		    //Receiver declaration
		    receiveChannel = connection.createChannel();
		    receiveChannel.exchangeDeclare(Constants.EXCHANGE_NAME, "direct");
		    String queueName = receiveChannel.queueDeclare().getQueue(); //Create arbitrarily named queue
		    receiveChannel.queueBind(queueName, Constants.EXCHANGE_NAME, lang); //Bind the queue to Exchange and key
		    
		    System.out.println(" [*] Waiting for messages directed to - " + lang);

		    QueueingConsumer consumer = new QueueingConsumer(receiveChannel);
		    receiveChannel.basicConsume(queueName, true, consumer);

		    while (true) {
		      QueueingConsumer.Delivery delivery = consumer.nextDelivery();
		      String message = new String(delivery.getBody());
		      String routingKey = delivery.getEnvelope().getRoutingKey();

		      System.out.println(" [x] Received in receiver: " + lang + "'" + routingKey + "':'" + message + "'");
		      MessageStats.receivedLangQ(lang);
		    }
		} catch (Exception e) {
			System.out.println("******************* Lang Processor failed *******************");
			e.printStackTrace();
		} finally {
			System.out.println("******************* Releasing resources *******************");
			if (receiveChannel != null) {
				try {
					receiveChannel.close();
				} catch (IOException e) {
					//Ignore
				}
			}
		}
	}

}
