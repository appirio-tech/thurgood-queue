package com.cloudspokes.squirrelforce;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloudspokes.exception.ProcessException;
import com.cloudspokes.squirrelforce.services.GitterUp;
import com.cloudspokes.thurgood.Thurgood;
import com.cloudspokes.thurgood.ThurgoodFactory;

public class Tester {

  private BufferedReader console = null;

  public static void main(String[] args) {
    Tester m = new Tester();
    m.run();
  }

  public void run() {

    showMenu();
    console = new BufferedReader(new InputStreamReader(System.in));

    try {

      String choice = console.readLine();

      while ((choice != null) && (Integer.parseInt(choice) != 99)) {

        if (Integer.parseInt(choice) == 1) {

          String results = GitterUp
              .unzipToGit(
                  "http://cs-public.s3.amazonaws.com/squirrelforce/jenkins-test.zip",
                  "jenkins-test",
                  new File("./src/main/webapp/WEB-INF/shells/apex"));

          System.out.println(results);
        } else if (Integer.parseInt(choice) == 2) {   
          
          String message = "{\"job_id\":\"52a6f0c162271a020000000e\",\"type\":\"java\"}";
          JSONObject jsonMessage = new JSONObject(message);          
          String jobId = jsonMessage.getString("job_id");
          Thurgood t = new ThurgoodFactory().getTheJudge("apex");          
          t.init(jobId);
          t.writeBuildPropertiesFile();          
          t.writeCloudspokesPropertiesFile();
          t.writeLog4jXmlFile();    
          System.out.println("DONE!!");
          
          
        } else if (Integer.parseInt(choice) == 3) {          
          writeLog4jXmlFile("logs.papertrailapp.com:24214");
        } else if (Integer.parseInt(choice) == 4) {          
          getPapertrailSystem("a0AK00000076XgBMAU");
        } else if (Integer.parseInt(choice) == 5) {
          sendMessage("1f69e4efcd1fd5d451c44d5f7a0de586","my message!!");

        } else if (Integer.parseInt(choice) == 6) {          
          
          Thurgood t = new ThurgoodFactory().getTheJudge("APEX");
          t.init("52a6f0c162271a020000000e");
          t.sendMessageToLogger("Test from Tester!");
          
        }

        showMenu();
        choice = console.readLine();

      }

    } catch (IOException io) {
      io.printStackTrace();
      System.out.println(io.getMessage());
    } catch (NumberFormatException nf) {
      run();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }

  } 

  private void showMenu() {
    System.out.println("\n1. Unzip to Git");
    System.out.println("2. Run the LangReceiver code (full monty)");
    System.out.println("3. Write Log4j File");
    System.out.println("4. Get System");
    System.out.println("5. Send Message");
    System.out.println("6. Get Job");
    System.out.println("99. Exit");
    System.out.println(" ");
    System.out.println("Operation: ");
  }
  
  protected void sendMessage(String jobId, String text) {

    String output;
    JSONObject results = null;

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpPost postRequest = new HttpPost(System.getenv("THURGOOD_API_URL")
        + "/jobs/" + jobId + "/message");
    postRequest.setHeader(new BasicHeader("Authorization", "Token token="
        + System.getenv("THURGOOD_API_KEY")));
    postRequest.addHeader("content-type", "application/json");
    postRequest.addHeader("accept", "application/json");
    
    try {
      
      JSONObject keyArgs = new JSONObject();
      keyArgs.put("text", text);
      keyArgs.put("sender", "thurgood-queue");
      
      JSONObject msgArg = new JSONObject();
      msgArg.put("message", keyArgs);
      StringEntity input = new StringEntity(msgArg.toString());
      postRequest.setEntity(input);

      HttpResponse response = httpClient.execute(postRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      while ((output = br.readLine()) != null) {
        results = new JSONObject(output);
        break;
      }
      httpClient.getConnectionManager().shutdown();  
      
      if (!results.getString("response").equals("true")) throw new ProcessException("Error sending message! Not Sent."); 

    } catch (JSONException e) {
      throw new ProcessException(
          "Error sending message. Could not parse JSON.");
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood logger info.");     
    }    
    
  }  
  
  private void getPapertrailSystem(String participantId) 
      throws ClientProtocolException, IOException, JSONException {
    
    System.out.println("Fetching Papertrail system at " 
        + System.getenv("CS_API_URL") + "....");
    
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(
        System.getenv("CS_API_URL") + "/squirrelforce/system/"
            + participantId);
    getRequest.addHeader("accept", "application/json");
    getRequest.setHeader(new BasicHeader("Authorization", "Token token=" + System.getenv("CS_API_KEY")));
    HttpResponse response = httpClient.execute(getRequest);
    BufferedReader br = new BufferedReader(new InputStreamReader(
        (response.getEntity().getContent())));
    String output;
    JSONObject payload = null;
    
    while ((output = br.readLine()) != null) {
      payload = new JSONObject(output).getJSONObject("response");
      break;
    }
    System.out.println(payload.getString("syslog_hostname"));
    System.out.println(payload.getInt("syslog_port"));
    
  }
  
  private void writeLog4jXmlFile(String syslogHost) {
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
            // check for replacement
            if (line.indexOf("{{SYSLOGHOST}}",0) != -1) {
              line = line.replace("{{SYSLOGHOST}}", syslogHost);
            }
            out.write(line + "\r\n");
        }

    } catch (IOException e) {
        throw new RuntimeException(e);

    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
    
  @SuppressWarnings("unused")
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

}
