package com.cloudspokes.thurgood;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloudspokes.exception.ProcessException;
import com.cloudspokes.squirrelforce.services.GitterUp;

public abstract class Thurgood {

  protected static final String SHELLS_DIRECTORY = "./src/main/webapp/WEB-INF/shells";

  String submissionType;
  String submissionUrl;
  String participantId;
  String memberName;
  int challengeId;
  Server server;
  PapertrailSystem papertrailSystem;
  Job job;
  
  public void init(String jobId) throws ProcessException {
    try {

      this.job = new Job(getJob(jobId));
      this.submissionUrl = job.codeUrl;
      this.memberName = job.userId; 

      JSONObject options = new JSONObject(job.options);
      this.participantId = options.getString("participant_id");
      this.challengeId = options.getInt("challenge_id");      
      
    } catch (JSONException e) {
      throw new ProcessException(
          "Error returning Thurgood job options info. Could not parse JSON for options. Not found.");
    }
    
    System.out.println("Processing job: " + job.jobId);
    sendMessageToLogger("Processing language specific job for Thurgood queue.");
    
    ensureZipFile();
    getServer();
    getLoggerSystem();

  }  

  private void ensureZipFile() throws ProcessException {

    if (submissionUrl.lastIndexOf('.') > 0) {
      String extension = submissionUrl.substring(
          submissionUrl.lastIndexOf('.') + 1, submissionUrl.length());
      System.out.println("Submission file extension: " + extension);
      if (!extension.equalsIgnoreCase("zip")) {
        sendMessageToLogger("Unsupported file type: " + extension);
        throw new ProcessException("Unsupported file type: " + extension);
      }
    } else {
      throw new ProcessException("Unsupported file type: Unknown");
    }

  }
  
  private JSONObject getJob(String jobId) throws ProcessException {

    String output;
    JSONObject job = null;

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(System.getenv("THURGOOD_API_URL")
        + "/jobs/" + jobId);
    getRequest.setHeader(new BasicHeader("Authorization", "Token token="
        + System.getenv("THURGOOD_API_KEY")));
    getRequest.addHeader("accept", "application/json");

    try {

      HttpResponse response = httpClient.execute(getRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      while ((output = br.readLine()) != null) {
        job = new JSONObject(output).getJSONObject("response");
        break;
      }
      httpClient.getConnectionManager().shutdown();
      
      return job;

    } catch (JSONException e) {
      throw new ProcessException(
          "Error returning Thurgood job info. Could not parse JSON.");
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood job info.");
    }

  }    
  
  private void getServer() throws ProcessException {

    String output;
    JSONObject s = null;

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(System.getenv("THURGOOD_API_URL")
        + "/jobs/" + this.job.jobId + "/server");
    getRequest.setHeader(new BasicHeader("Authorization", "Token token="
        + System.getenv("THURGOOD_API_KEY")));
    getRequest.addHeader("accept", "application/json");

    try {

      HttpResponse response = httpClient.execute(getRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      while ((output = br.readLine()) != null) {
        s = new JSONObject(output).getJSONObject("response");
        break;
      }
      httpClient.getConnectionManager().shutdown();
      
      server = new Server();
      server.id = String.valueOf(s.getInt("id"));
      server.platform = s.getString("platform");
      server.username = s.getString("username");
      server.supportedLanguages = s.getString("languages");
      server.repoName = s.getString("repo_name");
      server.instanceUrl = s.getString("instance_url");
      server.password = s.getString("password");
      
      sendMessageToLogger("Successfully fetched Thurgood testing server info.");

    } catch (JSONException e) {
      throw new ProcessException(
          "Error returning Thurgood server info. Could not parse JSON.");
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood server info.");
    }

  }     
  
  private void getLoggerSystem() throws ProcessException {

    String output;
    JSONObject s = null;

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(System.getenv("THURGOOD_API_URL")
        + "/jobs/" + this.job.jobId + "/logger");
    getRequest.setHeader(new BasicHeader("Authorization", "Token token="
        + System.getenv("THURGOOD_API_KEY")));
    getRequest.addHeader("accept", "application/json");

    try {

      HttpResponse response = httpClient.execute(getRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      while ((output = br.readLine()) != null) {
        s = new JSONObject(output).getJSONObject("response");
        break;
      }
      httpClient.getConnectionManager().shutdown();
      
      papertrailSystem = new PapertrailSystem();
      papertrailSystem.id = String.valueOf(s.getInt("id"));
      papertrailSystem.syslogPort = s.getInt("syslog_port");
      papertrailSystem.name = s.getString("name");
      papertrailSystem.syslogHostName = s.getString("syslog_hostname"); 
      
      sendMessageToLogger("Successfully fetched Papertrail logger info.");

    } catch (JSONException e) {
      throw new ProcessException(
          "Error returning Thurgood logger info. Could not parse JSON.");
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood logger info.");
    }

  }       

  public void writeLog4jXmlFile() throws ProcessException {

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

        if (line.indexOf("{{PARTICIPANT_ID}}", 0) != -1)
          line = line.replace("{{PARTICIPANT_ID}}", participantId);

        out.write(line + "\r\n");
      }
      System.out.println("Successfully wrote log4j.xml");
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
    sendMessageToLogger("Pusing files, assets, jars, etc. to git repo....");
    return GitterUp.unzipToGit(submissionUrl, server.repoName, langShellFolder);
  }
  
  public void sendMessageToLogger(String text) {

    String output;
    JSONObject results = null;

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
  
  public abstract void writeCloudspokesPropertiesFile() throws ProcessException;

  public abstract void writeBuildPropertiesFile() throws ProcessException;  

  protected class Server {

    String id;
    String platform;
    String username;
    String supportedLanguages;
    String repoName;
    String instanceUrl;
    String installedServices;
    String password;
    String operatingSystem;
    
    Server() {}

    Server(JSONObject s) throws JSONException {
      this.id = s.getString("id");
      this.platform = s.getString("platform");
      this.username = s.getString("username");
      this.supportedLanguages = s.getString("supported_programming_language");
      this.repoName = s.getString("repo_name");
      this.instanceUrl = s.getString("instance_url");
      this.password = s.getString("password");
    }

  }

  protected class PapertrailSystem {

    String id;
    int syslogPort;
    String syslogHostName;
    String name;
    
    PapertrailSystem() {}

    PapertrailSystem(JSONObject s) throws JSONException {
      this.id = s.getString("id");
      this.syslogPort = s.getInt("syslog_port");
      this.name = s.getString("name");
      this.syslogHostName = s.getString("syslog_hostname");
    }

  }
  
  protected class Job {

    String jobId;
    String codeUrl;
    String language;
    String platform;
    String userId;
    String options;
    
    Job(JSONObject j) throws JSONException {
      this.jobId = j.getString("job_id");
      this.codeUrl = j.getString("code_url");
      this.language = j.getString("language");
      this.platform = j.getString("platform");
      this.userId = j.getString("user_id");
      this.options = j.getString("options");
    }

  }  

}
