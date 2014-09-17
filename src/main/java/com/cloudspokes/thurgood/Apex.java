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
    
    if (server == null) {
      System.out.println("[FATAL] No server assigned. Cannot write build.properties file for job " + this.job.jobId); 
      throw new ProcessException("No Server assigned to this job.");
    }    
    
    BufferedWriter out = null;
    String file_name = SHELLS_DIRECTORY + "/apex/build.properties";
    
    try {
      
      FileWriter fstream = new FileWriter(file_name);
      out = new BufferedWriter(fstream);
      out.write("sf.username = " + server.username + "\n");
      out.write("sf.password = " + server.password + "\n");
      out.write("sf.serverurl = " + server.instanceUrl);
      System.out.println("[INFO] Successfully wrote build.properties for job " + this.job.jobId);
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
    
    if (server == null) {
      System.out.println("[FATAL] No server assigned. Cannot write topcoder.properties file for job " + this.job.jobId); 
      throw new ProcessException("No Server assigned to this job.");
    }
    
    BufferedWriter out = null;
    String file_name = SHELLS_DIRECTORY + "/apex/cloudspokes.properties";
    
    try {    

      FileWriter fstream = new FileWriter(file_name);
      out = new BufferedWriter(fstream);       
      out.write("job_id=" + job.jobId + "\n");
      out.write("steps=" + job.steps);
      System.out.println("[INFO] Successfully wrote topcoder.properties for job " + this.job.jobId);  
      sendMessageToLogger("Successfully wrote topcoder.properties for job.");
      
    } catch (IOException e) {
      throw new ProcessException("IO Error creating topcoder.properties file.");
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
