package com.cloudspokes.thurgood;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.cloudspokes.exception.ProcessException;

/**
 * @author Jeff Douglas
 *
 */
public class Apex extends Thurgood {

  @Override
  public void writeBuildPropertiesFile() throws ProcessException {
    
    BufferedWriter out = null;
    String file_name = SHELLS_DIRECTORY + "/apex/build.properties";
    
    try {
      
      FileWriter fstream = new FileWriter(file_name);
      out = new BufferedWriter(fstream);
      out.write("sf.username = " + server.username + "\n");
      out.write("sf.password = " + server.password + "\n");
      out.write("sf.serverurl = " + server.instanceUrl);
      System.out.println("Successfully wrote build.properties");
      sendMessageToLogger("Successfully wrote build.properties for Apex deployment.");
    
    } catch (IOException e) {
        throw new ProcessException("IO Error creating build.properties file.");
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          System.out.println("Could not close writer");
        }
      }
    }       

  }

  @Override
  public void writeCloudspokesPropertiesFile() throws ProcessException {
    
    BufferedWriter out = null;
    String file_name = SHELLS_DIRECTORY + "/apex/cloudspokes.properties";
    
    try {    

      FileWriter fstream = new FileWriter(file_name);
      out = new BufferedWriter(fstream);
      if (memberName != null) {
        out.write("membername=" + memberName + "\n");
        out.write("challenge_id=" + challengeId + "\n");        
        out.write("s3_bucket=" + challengeId + '/' + memberName + "\n");
      } else {
        out.write("s3_bucket=" + job.jobId + "\n");
      }      
      out.write("job_id=" + job.jobId + "\n");
      out.write("api_key=" + System.getenv("THURGOOD_API_KEY"));
      System.out.println("Successfully wrote cloudspokes.properties"); 
      
    } catch (IOException e) {
      throw new ProcessException("IO Error creating cloudspokes.properties file.");
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          System.out.println("Could not close writer");
        }
      }
    }          

  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub

  }

}
