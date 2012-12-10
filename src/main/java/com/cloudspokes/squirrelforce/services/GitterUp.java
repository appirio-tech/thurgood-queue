package com.cloudspokes.squirrelforce.services;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.cloudspokes.squirrelforce.services.IOUtils;
import com.cloudspokes.squirrelforce.services.MultiSourceCommitService;

public class GitterUp {
  
  public static String unzipToGit(String url, String repoName) {
    
    String results = "";
    
    MultiSourceCommitService commitService = new MultiSourceCommitService();
    commitService.setCredentials("jeff@jeffdouglas.com","6G8}4Hm.9z");          
    
    File sourceZip = IOUtils.downloadFromUrlToTempFile("http://cs-production.s3.amazonaws.com/challenges/1884/wcheung/octavius3.zip");
    File overlayFolder = IOUtils.unzipToTempDir(sourceZip);
    List<File> folders = Collections.singletonList(overlayFolder);
    
    try {
      String repoUrl = commitService.commitFromFoldersToNewRepo(
          folders, repoName, false);
      results = "new repo created: " + repoUrl;

    } catch (Exception e) {
        results = "commitFromFoldersToNewRepo failed: " + e.getClass().getName() + " -  " + e.getMessage();  
    } finally {
        sourceZip.delete();
        IOUtils.deleteDir(overlayFolder);
    }       
    
    return results;
    
  }

}
