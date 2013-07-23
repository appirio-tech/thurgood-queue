package com.cloudspokes.thurgood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudspokes.exception.ProcessException;

public class ThurgoodTestChallenge {

  private static String jobId = null;
  private static String codeUrl = "http://www.myfiles.com/code.zip";
  private static String language = "Apex";
  private static String platform = "Salesforce.com";
  private static String userId = "jeffdonthemic";
  private static String email = "jdouglas@appirio.com";  
  private static String systemPapertrailId = "jeffdouglas-junit";  
  private static String challengeId = "2001";  
  private static String participantId = "a0IK00000058lO8";  
  
  
  @Test
  public void testInit() {
    Thurgood t = new ThurgoodFactory().getTheJudge("APEX");
    t.init(jobId);
    assertEquals(jobId, t.job.jobId);
    assertEquals(codeUrl, t.job.codeUrl);
    assertEquals(language, t.job.language);
    assertEquals(platform, t.job.platform);
    assertEquals(userId, t.job.userId);
    
    // check the server
    assertNotNull(t.server);
    assertEquals(platform.toLowerCase(), t.server.platform.toLowerCase());
    
    // check the logger
    assertNotNull(t.papertrailSystem);
    assertEquals(systemPapertrailId, t.papertrailSystem.papertrailId);  
    
    t.writeLog4jXmlFile();
    File log4j = new File("./src/main/webapp/WEB-INF/shells/apex/log4j.xml");
    assertEquals(true, log4j.exists());
    
    t.writeBuildPropertiesFile();
    File buildFile = new File("./src/main/webapp/WEB-INF/shells/apex/build.properties");
    assertEquals(true, buildFile.exists());  
    
    t.writeCloudspokesPropertiesFile();
    File cloudspokesFile = new File("./src/main/webapp/WEB-INF/shells/apex/cloudspokes.properties");
    assertEquals(true, cloudspokesFile.exists());     
    // make sure the values are correct
    Properties cloudspokesProp = new Properties();
    try {
      //load a properties file
      cloudspokesProp.load(new FileInputStream("./src/main/webapp/WEB-INF/shells/apex/cloudspokes.properties"));
      // make sure the values were set correctly
      assertEquals(challengeId+"/"+userId, cloudspokesProp.getProperty("s3_bucket"));
      assertEquals(userId, cloudspokesProp.getProperty("membername"));
      assertEquals(jobId, cloudspokesProp.getProperty("job_id"));
      assertEquals(System.getenv("THURGOOD_API_KEY"), cloudspokesProp.getProperty("api_key"));
      assertEquals(challengeId, cloudspokesProp.getProperty("challenge_id"));
    } catch (IOException ex) {
      ex.printStackTrace();
    }     
    
  }  
  
  @BeforeClass
  // Create a new job and submit it for processing
  public static void setUpBeforeClass() throws Exception {

    String output;
    JSONObject results = null;    

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpPost postRequest = new HttpPost(System.getenv("THURGOOD_API_URL") + "/jobs");
    postRequest.addHeader("content-type", "application/json");
    postRequest.addHeader("accept", "application/json");
    
    try {
      
      JSONObject keyArgs = new JSONObject();
      keyArgs.put("code_url", codeUrl);
      keyArgs.put("language", language);
      keyArgs.put("platform", platform);
      keyArgs.put("user_id", userId);
      keyArgs.put("email", email);
      
      JSONObject msgArg = new JSONObject();
      msgArg.put("job", keyArgs);
      StringEntity input = new StringEntity(msgArg.toString());
      postRequest.setEntity(input);

      HttpResponse response = httpClient.execute(postRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      while ((output = br.readLine()) != null) {
        results = new JSONObject(output).getJSONObject("response");
        System.out.println("New job: " + results);
        jobId = results.getString("job_id");
        break;
      }
      httpClient.getConnectionManager().shutdown();  
      

    } catch (JSONException e) {
      throw new ProcessException(
          "Error creating job. Could not parse JSON.");
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood info for new job.");     
    }     

    httpClient = new DefaultHttpClient();
    HttpPut putRequest = new HttpPut(System.getenv("THURGOOD_API_URL") + "/jobs/" +
        jobId + "/submit");
    putRequest.addHeader("content-type", "application/json");
    putRequest.addHeader("accept", "application/json");
    
    System.out.println("New job " + jobId);
    
    try {
      
      JSONObject keyArgs = new JSONObject();
      keyArgs.put("challenge_id", challengeId);
      keyArgs.put("participant_id", participantId);
      keyArgs.put("system_papertrail_id", systemPapertrailId);
      
      JSONObject msgArg = new JSONObject();
      msgArg.put("options", keyArgs);
      StringEntity input = new StringEntity(msgArg.toString());
      putRequest.setEntity(input);
      
      System.out.println(keyArgs);

      HttpResponse response = httpClient.execute(putRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      while ((output = br.readLine()) != null) {
        System.out.println(results);
        results = new JSONObject(output).getJSONObject("response");
        System.out.println("Submitted for processing: " + results);
        break;
      }
      httpClient.getConnectionManager().shutdown();  
      

    } catch (JSONException e) {
      throw new ProcessException(
          "Error submitting for processing. Error " + e.getMessage());
    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood info.");     
    }       
    
  }
  
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    
    String output;

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpGet getRequest = new HttpGet(System.getenv("THURGOOD_API_URL") + "/jobs/" + jobId + "/complete");
    getRequest.addHeader("content-type", "application/json");
    getRequest.addHeader("accept", "application/json");
    
    try {
      
      HttpResponse response = httpClient.execute(getRequest);

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (response.getEntity().getContent())));

      while ((output = br.readLine()) != null) {
        System.out.println("Marked job complete: " + output);
        break;
      }
      httpClient.getConnectionManager().shutdown();  

    } catch (IOException e) {
      throw new ProcessException("IO Error processing Thurgood info for new job.");     
    }      
    
  } 

}
