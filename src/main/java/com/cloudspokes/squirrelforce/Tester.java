package com.cloudspokes.squirrelforce;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloudspokes.squirrelforce.services.GitterUp;

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
          System.out.println("done");
        } else if (Integer.parseInt(choice) == 3) {          
          writeLog4jXmlFile("logs.papertrailapp.com:24214");
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
    System.out.println("2. Reserve server");
    System.out.println("3. Write Log4j File");
    System.out.println("99. Exit");
    System.out.println(" ");
    System.out.println("Operation: ");
  }
  
  private void writeLog4jXmlFile(String syslogHost) {
    PrintWriter out = null;
    String outputfile = "./src/main/webapp/WEB-INF/shells/apex/log4j.xml";
    try {
        URL log4jTemplate = new URL("http://cs-misc.s3.amazonaws.com/log4j.xml");
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
