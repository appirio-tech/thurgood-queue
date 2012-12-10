package com.cloudspokes.squirrelforce;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

public class MainReceiverSender implements Runnable {
	private Connection connection;

	public MainReceiverSender(Connection connection) {
		this.connection = connection;
	}
	
	public void run() {
		Channel sendChannel = null;
		Channel receiveChannel = null;

		try {
			receiveChannel = connection.createChannel();

			receiveChannel.queueDeclare(Constants.QUEUE_NAME, false, false, false, null);
			System.out.println(" [*] Waiting for messages.");
			QueueingConsumer consumer = new QueueingConsumer(receiveChannel);
			receiveChannel.basicConsume(Constants.QUEUE_NAME, true, consumer);

			// Sender declaration
			sendChannel = connection.createChannel();
			sendChannel.exchangeDeclare(Constants.EXCHANGE_NAME, "direct");

			while (true) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String message = new String(delivery.getBody());
				System.out.println(" [x] Received in mainQueue receiver: '"
						+ message + "'");
				// MessageStats.receivedInMainQ();

				JsonElement jsonElement = new JsonParser().parse(message);
				if (jsonElement.isJsonObject()) {
					JsonObject jsonObj = jsonElement.getAsJsonObject();
					String type = null;
					if (jsonObj.has("Type")) {
						type = jsonObj.get("Type").getAsString();
					}
					if (type == null) {
						System.out.println("Message does not contain Type. Cannot send out. Message: " + message);
					} else {
						//Route to appropriate queue based on received type (i.e. lang)
						sendChannel.basicPublish(Constants.EXCHANGE_NAME, type, null,
								message.getBytes());
						// MessageStats.sentInMainQ();
					}
				}
			}
		} catch (Exception e) {
			System.out
					.println("******************* Message Processor failed *******************");
			e.printStackTrace();
		} finally {
			System.out
					.println("******************* Releasing resources *******************");
			if (receiveChannel != null) {
				try {
					receiveChannel.close();
				} catch (IOException e) {
					// Ignore
				}
			}
			if (sendChannel != null) {
				try {
					sendChannel.close();
				} catch (IOException e) {
					// Ignore
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}
	
	public static void main(String[] args) {
		String str1 = "{\"name\":\"shashi\"}";
		String str2 = "";
		JsonElement jsonObj1 = new JsonParser().parse(str1);
		JsonElement jsonObj2 = new JsonParser().parse(str2);
		System.out.println("jsonObj2 is jsonobject: " + (jsonObj2.isJsonObject()));
		System.out.println("jsonObj1 is jsonobject: " + (jsonObj1.isJsonObject()));

	}

}
