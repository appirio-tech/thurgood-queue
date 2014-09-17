package com.cloudspokes.thurgood;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloudspokes.exception.ProcessException;
import com.cloudspokes.squirrelforce.services.GitterUp;

public abstract class Thurgood {

  protected static final String SHELLS_DIRECTORY = "./src/main/webapp/WEB-INF/shells";

  String submissionType;
  String submissionUrl;
  String handle;
  File tempZipFolder;
  Server server;
  PapertrailSystem papertrailSystem;
  Job job;

  public void init(String jobId) throws ProcessException {
    try {
      this.job = new Job(getJob(jobId));
      this.submissionUrl = job.codeUrl;
      this.handle = job.handle;
    } catch (JSONException e) {
      throw new ProcessException("Error returning Thurgood job: "
          + e.getMessage());
    }

    System.out.println("[INFO] Processing " + submissionType + " job: " + job.jobId);
    sendMessageToLogger("Processing language specific job for Thurgood queue.");

    getServer();
    getLoggerSystem();    

  }

  private JSONObject getJob(String jobId) throws ProcessException {

    String line;
    JSONObject job = null;

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(System.getenv("THURGOOD_API_URL")
        + "/jobs/" + jobId);
    getRequest.setHeader(new BasicHeader("Authorization", "Token token="
        + System.getenv("THURGOOD_API_KEY")));
    getRequest.addHeader("accept", "application/json");

    try {

      StringBuilder jsonString = new StringBuilder();
      HttpResponse response = httpClient.execute(getRequest);
      
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new Exception("Thurgood API returned a status of " + response.getStatusLine().getStatusCode());
      }
      
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      // build the json into a string
      while ((line = br.readLine()) != null) {
        jsonString.append(line);
      }

      // parse the string into a json object
      JSONObject json = new JSONObject(jsonString.toString());
      if (json.getBoolean("success")) {
        JSONArray data = json.getJSONArray("data");
        job = new JSONObject(data.get(0).toString());
      } else {
        throw new Exception("Failure fetching Job.");
      }

      return job;

    } catch (JSONException e) {
      throw new ProcessException(
          "Error returning Thurgood job info. Could not parse JSON: "
              + e.getMessage());
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood job info: "
          + e.getMessage());
    } catch (Exception e) {
      throw new ProcessException(e.getMessage());
    } finally {
      httpClient.getConnectionManager().shutdown();
    }

  }

  private void getServer() throws ProcessException {

    String line;
    DefaultHttpClient httpClient = new DefaultHttpClient();
    String q = "{\"jobId\":\"" + this.job.jobId + "\"}";

    try {

      HttpGet getRequest = new HttpGet(System.getenv("THURGOOD_API_URL")
          + "/servers?q=" + URLEncoder.encode(q, "UTF-8"));
      getRequest.setHeader(new BasicHeader("Authorization", "Token token="
          + System.getenv("THURGOOD_API_KEY")));
      getRequest.addHeader("accept", "application/json");

      StringBuilder jsonString = new StringBuilder();
      HttpResponse response = httpClient.execute(getRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      // build the json into a string
      while ((line = br.readLine()) != null) {
        jsonString.append(line);
      }

      // parse the string into a json object
      JSONObject json = new JSONObject(jsonString.toString());
      // get the actual data
      JSONArray data = json.getJSONArray("data");

      if (json.getBoolean("success")) {
        // if there is actually a server assigned for the job
        if (data.length() > 0) {
          server = new Server(new JSONObject(data.get(0).toString()));
          sendMessageToLogger("Successfully fetched Thurgood testing server info.");
        } else {
          System.out.println("[WARN] No server found for job " + this.job.jobId);
        }
      } else {
        throw new Exception("Failure fetching Server.");
      }

    } catch (JSONException e) {
      throw new ProcessException(
          "Error returning Thurgood server info. Could not parse JSON:" + e.getMessage());
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood server info: "
          + e.getMessage());
    } catch (Exception e) {
      throw new ProcessException(e.getMessage());
    } finally {
      httpClient.getConnectionManager().shutdown();
    }

  }

  private void getLoggerSystem() throws ProcessException {
    
    if (this.job.loggerId == null) { return; }

    String line;

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(System.getenv("THURGOOD_API_URL")
        + "/loggers/" + this.job.loggerId);
    getRequest.setHeader(new BasicHeader("Authorization", "Token token="
        + System.getenv("THURGOOD_API_KEY")));
    getRequest.addHeader("accept", "application/json");

    try {

      StringBuilder jsonString = new StringBuilder();
      HttpResponse response = httpClient.execute(getRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      // build the json into a string
      while ((line = br.readLine()) != null) {
        jsonString.append(line);
      }
      
      // parse the string into a json object
      JSONObject json = new JSONObject(jsonString.toString());
      // get the actual data
      JSONArray data = json.getJSONArray("data");      
      
      if (json.getBoolean("success")) {
        if (data.length() > 0) {
          papertrailSystem = new PapertrailSystem(new JSONObject(data.get(0).toString()));
          sendMessageToLogger("Successfully fetched Papertrail logger info.");
        }
      } else {
        throw new Exception("Failure fetching Papertrail System.");
      }      

    } catch (JSONException e) {
      throw new ProcessException(
          "Error returning Thurgood logger info. Could not parse JSON.");
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood logger info.");
    } catch (Exception e) {
      throw new ProcessException(e.getMessage());
    } finally {
      httpClient.getConnectionManager().shutdown();
    }

  }

  public void writeLog4jXmlFile() throws ProcessException {
    
    if (server == null) {
      System.out.println("[FATAL] No server assigned. Cannot write log4j file for job " + this.job.jobId); 
      throw new ProcessException("No Server assigned to this job.");
    }    

    PrintWriter out = null;
    String outputfile = SHELLS_DIRECTORY + "/" + submissionType + "/log4j.xml";

    try {      
      URL log4jTemplate = new URL(
          "http://cs-thurgood.s3.amazonaws.com/log4j.xml");
      File outFile = new File(outputfile);
      out = new PrintWriter(new FileWriter(outFile));
      // read the xml template in from the url
      BufferedReader reader = new BufferedReader(new InputStreamReader(
          log4jTemplate.openStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        // check for replacements
        if (line.indexOf("{{SYSLOGHOST}}", 0) != -1)
          line = line.replace("{{SYSLOGHOST}}", papertrailSystem.syslogHostName
              + ":" + papertrailSystem.syslogPort);
        
        // add in the job id
        line = line.replace("{{JOB_ID}}", job.jobId);

        out.write(line + "\r\n");
      }
      System.out.println("[INFO] Successfully wrote log4j.xml for job " + this.job.jobId);
      sendMessageToLogger("Successfully wrote log4j.xml to attach logger to Papertrail.");

    } catch (IOException e) {
      throw new ProcessException("IO Error creating log4j xml file.");
    } finally {
      if (out != null) {
        out.close();
      }
    }

  }

  public String pushFilesToGit(File langShellFolder) {
    sendMessageToLogger("Pushing files, assets, jars, etc. to git repo....");
    return GitterUp.unzipToGit(submissionUrl, server.repoName, langShellFolder);
  }

  public void sendMessageToLogger(String text) {

    String line;
    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpPost postRequest = new HttpPost(System.getenv("THURGOOD_API_URL")
        + "/jobs/" + this.job.jobId + "/message");
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

      StringBuilder jsonString = new StringBuilder();
      HttpResponse response = httpClient.execute(postRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      while ((line = br.readLine()) != null) {
        jsonString.append(line);
      }
      
      // parse the string into a json object
      JSONObject json = new JSONObject(jsonString.toString());
      
      if (!json.getBoolean("success")) {
        throw new ProcessException("Error sending message! Not Sent.");
      }       

    } catch (JSONException e) {
      throw new ProcessException(
          "Error sending message. Could not parse JSON: " + e.getMessage());
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood logger info.");     
    } finally {
      httpClient.getConnectionManager().shutdown();
    }
    
  }
  
  public void cleanupFailedSubmit() {

    String line;

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(System.getenv("THURGOOD_API_URL")
        + "/jobs/" + this.job.jobId + "/complete");
    getRequest.setHeader(new BasicHeader("Authorization", "Token token="
        + System.getenv("THURGOOD_API_KEY")));
    getRequest.addHeader("accept", "application/json");

    try {

      StringBuilder jsonString = new StringBuilder();
      HttpResponse response = httpClient.execute(getRequest);      
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      // build the json into a string
      while ((line = br.readLine()) != null) {
        jsonString.append(line);
      }

      // parse the string into a json object
      JSONObject json = new JSONObject(jsonString.toString());
      if (json.getBoolean("success")) {
        System.out.println("[INFO] Marking failed job as complete.");
      } else {
        System.out.println("[INFO] Unable to mark failed job as complete.");
      }

    } catch (JSONException e) {
      throw new ProcessException(
          "Error returning Thurgood job complete info. Could not parse JSON: "
              + e.getMessage());
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood job complete info: "
          + e.getMessage());
    } catch (Exception e) {
      throw new ProcessException(e.getMessage());
    } finally {
      httpClient.getConnectionManager().shutdown();
    }
    
  }  

  public abstract void writeCloudspokesPropertiesFile() throws ProcessException;

  public abstract void writeBuildPropertiesFile() throws ProcessException;

  protected class Server {

    String id;
    String platform;
    String username;
    ArrayList<String> supportedLanguages;
    String repoName;
    String instanceUrl;
    String installedServices;
    String password;
    String operatingSystem;

    Server() {}

    Server(JSONObject s) throws JSONException {
      this.id = s.getString("_id");
      this.platform = s.optString("platform", "").toLowerCase();
      this.username = s.optString("username", "");
      this.repoName = s.optString("repoName", "");
      this.instanceUrl = s.optString("instanceUrl", "");
      this.password = s.optString("password", "");
      // convert the languages into an array
      this.supportedLanguages = new ArrayList<String>();
      JSONArray jArray = (JSONArray) s.getJSONArray("languages");
      if (jArray != null) {
        for (int i = 0; i < jArray.length(); i++) {
          supportedLanguages.add(jArray.get(i).toString().toLowerCase());
        }
      }

    }

  }

  protected class PapertrailSystem {

    String id;
    int syslogPort;
    String syslogHostName;
    String name;
    String papertrailId;

    PapertrailSystem() {
    }

    PapertrailSystem(JSONObject s) throws JSONException {
      this.id = s.getString("_id");
      this.syslogPort = s.getInt("syslogPort");
      this.name = s.getString("name");
      this.papertrailId = s.getString("papertrailId");
      this.syslogHostName = s.getString("syslogHostname");
    }

  }

  protected class Job {

    String jobId;
    String codeUrl;
    String language;
    String platform;
    String handle;
    String loggerId;
    String steps;    
    JSONObject options;

    Job(JSONObject j) throws JSONException {
      this.jobId = j.getString("_id");
      this.codeUrl = j.optString("codeUrl", "");
      this.language = j.optString("language", "").toLowerCase();
      this.platform = j.optString("platform", "").toLowerCase();
      this.handle = j.optString("userId", "");
      this.loggerId = j.optString("loggerId", null);
      this.steps = j.optString("steps", null);
      JSONArray jsonOptions = j.getJSONArray("options");
      if (jsonOptions.length() > 0)
        options = new JSONObject(jsonOptions.get(0).toString());
    }

  }

}
