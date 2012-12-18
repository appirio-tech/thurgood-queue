package com.cloudspokes.squirrelforce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloudspokes.squirrelforce.services.GitterUp;
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

        // parse the json
        JSONObject jsonMessage = new JSONObject(message);
        String submissionUrl = jsonMessage.getString("url");
        
        // reserve a server and then use the configuration
        JSONObject server = getSquirrelforceServer("jeffdonthemic");
        
        if (server != null) {
          
          System.out.println("Reserved Server: " + server.getString("name"));
          System.out.println("Username: " + server.getString("username"));
          System.out.println("Password: " + server.getString("password"));
          System.out.println("Repo: " + server.getString("repo_name"));
          
          System.out.println("Processing submission " + jsonMessage.getString("name")
              + " with code from " + submissionUrl);
          System.out.println("Kicking off GitterUp...");

          String results = GitterUp.unzipToGit(submissionUrl, server.getString("repo_name"));
          System.out.println(results);     
          
        } else {
          System.out.println("Could not get a server");
        }

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

  private JSONObject getSquirrelforceServer(String membername)
      throws ClientProtocolException, IOException, JSONException {

    System.out.println("Reserving Squirrelforce server....");
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(
        "http://cs-api-sandbox.herokuapp.com/v1/squirrelforce/reserve_server?membername="
            + membername);
    getRequest.addHeader("accept", "application/json");
    HttpResponse response = httpClient.execute(getRequest);
    BufferedReader br = new BufferedReader(new InputStreamReader(
        (response.getEntity().getContent())));
    String output;
    JSONObject payload = null;
    ;
    while ((output = br.readLine()) != null) {
      // System.out.println(output);
      payload = new JSONObject(output).getJSONObject("response");
      break;
    }
    httpClient.getConnectionManager().shutdown();

    JSONObject server = null;

    if (payload.getBoolean("success")) {
      server = payload.getJSONObject("server");
    } else {
      System.out.println(payload.getString("message"));
    }

    return server;

  }

}
