package com.cloudspokes.squirrelforce;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloudspokes.squirrelforce.services.GitterUp;
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

        System.out.println(" [x] Received in receiver: " + lang + "'"
            + routingKey + "':'" + message + "'");

        // parse the json
        JSONObject jsonMessage = new JSONObject(message);
        String submissionUrl = jsonMessage.getString("url");
        String participantId = jsonMessage.getString("challenge_participant");
        String membername = jsonMessage.getString("membername");
        int challenge_id = jsonMessage.getInt("challenge_id");

        // reserve a server and then use the configuration
        JSONObject server = getSquirrelforceServer(membername);
        // get the participant's papertrail system info for the logger
        JSONObject papertrailSystem = getPapertrailSystem(participantId);
        
        // create the build.properties file in the shells dir
        writeApexBuildProperties(server);
        // create the log4j file for papertrail
        writeLog4jXmlFile(papertrailSystem.getString("syslog_hostname"), 
            papertrailSystem.getInt("syslog_port"), participantId);
        // create the properties files with challange specific info
        writeCloudspokesProperties(membername, challenge_id);

        if (server != null) {

          System.out.println("Reserved Server: " + server.getString("name"));
          System.out.println("Repo: " + server.getString("repo_name"));

          System.out.println("Processing submission "
              + jsonMessage.getString("name") + " with code from "
              + submissionUrl);
          System.out.println("Kicking off GitterUp...");

          String results = GitterUp.unzipToGit(submissionUrl,
              server.getString("repo_name"), langShellFolder);
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

  private void writeApexBuildProperties(JSONObject server) throws IOException,
      JSONException {

    String file_name = "./src/main/webapp/WEB-INF/shells/apex/build.properties";
    FileWriter fstream = new FileWriter(file_name);
    BufferedWriter out = new BufferedWriter(fstream);
    out.write("sf.username = " + server.get("username") + "\n");
    out.write("sf.password = " + server.get("password") + "\n");
    out.write("sf.serverurl = " + server.get("instance_url"));
    out.close();
    System.out.println("Successfully wrote build.properties");

  }
  
  private void writeCloudspokesProperties(String membername, int challenge_id) throws IOException {
    
    String file_name = "./src/main/webapp/WEB-INF/shells/apex/cloudspokes.properties";
    FileWriter fstream = new FileWriter(file_name);
    BufferedWriter out = new BufferedWriter(fstream);
    out.write("membername= " + membername + "\n");
    out.write("challenge_id= " + challenge_id + "\n");
    out.close();
    System.out.println("Successfully wrote cloudspokes.properties");
    
  }  
  
  private void writeLog4jXmlFile(String hostname, int port, String participantId) {
  
    PrintWriter out = null;
    String outputfile = "./src/main/webapp/WEB-INF/shells/apex/log4j.xml";
    try {
        URL log4jTemplate = new URL("http://squirrelforce.herokuapp.com/log4j.xml");
        File outFile = new File(outputfile);
        out = new PrintWriter(new FileWriter(outFile));
        // read the xml template in from the url
        BufferedReader reader = new BufferedReader(new InputStreamReader(log4jTemplate.openStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            // check for replacements
            if (line.indexOf("{{SYSLOGHOST}}",0) != -1)
              line = line.replace("{{SYSLOGHOST}}", hostname + ":" + port);

            if (line.indexOf("{{PARTICIPANT_ID}}",0) != -1)
              line = line.replace("{{PARTICIPANT_ID}}", participantId);

            out.write(line + "\r\n");
        }

    } catch (IOException e) {
        throw new RuntimeException(e);

    } finally {
      if (out != null) {
        out.close();
      }
    }
    System.out.println("Successfully wrote log4j.xml");
  
  }  

  private JSONObject getSquirrelforceServer(String membername)
      throws ClientProtocolException, IOException, JSONException {

    System.out.println("Reserving Squirrelforce server at " 
        + System.getenv("CS_API_URL") + "....");
    
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(
        System.getenv("CS_API_URL") + "/squirrelforce/reserve_server?membername="
            + membername);
    getRequest.setHeader(new BasicHeader("Authorization", 
        "Token token=" + System.getenv("CS_API_KEY")));
    getRequest.addHeader("accept", "application/json");
    HttpResponse response = httpClient.execute(getRequest);
    BufferedReader br = new BufferedReader(new InputStreamReader(
        (response.getEntity().getContent())));
    String output;
    JSONObject payload = null;
    
    while ((output = br.readLine()) != null) {
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
  
  private JSONObject getPapertrailSystem(String participantId) 
      throws ClientProtocolException, IOException, JSONException {
    
    System.out.println("Fetching Papertrail system at " 
        + System.getenv("CS_API_URL") + "....");
    
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(
        System.getenv("CS_API_URL") + "/squirrelforce/system/"
            + participantId);
    getRequest.setHeader(new BasicHeader("Authorization", 
        "Token token=" + System.getenv("CS_API_KEY")));
    getRequest.addHeader("accept", "application/json");
    HttpResponse response = httpClient.execute(getRequest);
    BufferedReader br = new BufferedReader(new InputStreamReader(
        (response.getEntity().getContent())));
    String output;
    JSONObject payload = null;
    
    while ((output = br.readLine()) != null) {
      payload = new JSONObject(output).getJSONObject("response");
      break;
    }
    System.out.println(payload);
    return payload;
    
  }  

}
