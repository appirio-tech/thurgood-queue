package com.cloudspokes.squirrelforce;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloudspokes.exception.ProcessException;
import com.cloudspokes.thurgood.Thurgood;
import com.cloudspokes.thurgood.ThurgoodFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

public class LangReceiver implements Runnable {

  private static final String RELATIVE_PATH_OF_SHELLS_FOLDER_UNDER_WEBAPP_DIR = "WEB-INF/shells/";

  private String lang = null;
  private Connection connection = null;

  /**
   * The <i>shell folder</i> for the language of this receiver, whose contents
   * will be included in all uploads for this language, in addition to any
   * dynamic content.
   */
  private final File langShellFolder;

  public LangReceiver(String lang, Connection connection) {
    this.lang = lang;
    this.connection = connection;

    langShellFolder = VirtualFile
        .fromRelativePath(RELATIVE_PATH_OF_SHELLS_FOLDER_UNDER_WEBAPP_DIR
            + lang);
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
        String jobId = null;

        System.out.println(" [x] Received in receiver: " + lang + "'"
            + routingKey + "':'" + message + "'"); 
        
        try {
          
          // parse the json in the message
          JSONObject jsonMessage = new JSONObject(message);
          
          if (jsonMessage.has("job_id"))
            jobId = jsonMessage.getString("job_id");
          
          // create a new processor by type of language
          Thurgood t = new ThurgoodFactory().getTheJudge(lang);          
          
          if (jobId != null) {
            String submissionUrl = jsonMessage.getString("url");
            String participantId = jsonMessage.getString("challenge_participant");
            String memberName = jsonMessage.getString("membername");
            int challengeId = jsonMessage.getInt("challenge_id"); 
            
            // init, ensure zip file, reserve server & get papertrail system
            t.init(challengeId, memberName, submissionUrl,participantId);
          } else {
            t.init(jobId);
          }
          
          // build the language type specific files
          t.writeBuildPropertiesFile();          
          t.writeCloudspokesPropertiesFile();
          t.writeLog4jXmlFile();
          // push all of the files to github including the shells folder
          String results = t.pushFilesToGit(langShellFolder);
          System.out.println(results);          
             
        } catch (ProcessException e) {
          System.out.println(e.getMessage());     
        } catch (JSONException e) {
          System.out.println(e.getMessage());            
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

}
