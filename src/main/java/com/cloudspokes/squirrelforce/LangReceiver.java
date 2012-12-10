package com.cloudspokes.squirrelforce;

import java.io.IOException;

import com.cloudspokes.squirrelforce.services.GitterUp;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

import org.json.JSONObject;

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
      // Receiver declaration
      receiveChannel = connection.createChannel();
      receiveChannel.exchangeDeclare(Constants.EXCHANGE_NAME, "direct");
      // Create arbitrarily named queue
      String queueName = receiveChannel.queueDeclare().getQueue();
      // Bind the queue to Exchange and key
      receiveChannel.queueBind(queueName, Constants.EXCHANGE_NAME, lang);

      System.out.println(" [*] Waiting for messages directed to - " + lang);

      QueueingConsumer consumer = new QueueingConsumer(receiveChannel);
      receiveChannel.basicConsume(queueName, true, consumer);

      while (true) {
        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        String message = new String(delivery.getBody());
        String routingKey = delivery.getEnvelope().getRoutingKey();

        System.out.println(" [x] Received in receiver: " + lang + "'"
            + routingKey + "':'" + message + "'");
        // MessageStats.receivedLangQ(lang);

        // parse the json
        JSONObject jsonMessage = new JSONObject(message);
        String submissionUrl = jsonMessage.getString("url");
        String submissionName = jsonMessage.getString("Name");
        String submissionId = jsonMessage.getString("ID");

        System.out.println("Processing submission " + submissionName
            + " with code " + submissionUrl);
        System.out.println("Kicking off GitterUp...");

        String results = GitterUp.unzipToGit(submissionUrl, submissionName);
        System.out.println(results);

      }
    } catch (Exception e) {
      System.out
          .println("******************* Lang Processor failed *******************");
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
    }
  }

}
