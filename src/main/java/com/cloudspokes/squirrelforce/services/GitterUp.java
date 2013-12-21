package com.cloudspokes.squirrelforce.services;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.cloudspokes.exception.ProcessException;

public class GitterUp {

  public static String unzipToGit(String zipUrl, String repoName, File shellFolder) throws ProcessException {

    System.out.println("Pushing files to github...");
    String results = "";

    MultiSourceCommitService commitService = new MultiSourceCommitService();
    commitService.setCredentials(System.getenv("GIT_USERNAME"),
        System.getenv("GIT_PASSWORD"), System.getenv("GIT_OWNER"));
    
    File sourceZip = null;
    File tempZipFolder = null;
    
    // downlod the actual code files
    try {
      sourceZip = IOUtils.downloadFromUrlToTempFile(zipUrl);
      // will throw an exception here if not a .zip file
      tempZipFolder = IOUtils.unzipToTempDir(sourceZip);
    } catch (Exception e) {
      deleteSourceFile(sourceZip);
      return "Unable to unzip source code: " + e.getMessage();
    }
    
    List<File> folders = Arrays.asList(shellFolder, tempZipFolder);

    try {
      String repoUrl = commitService.commitFromFoldersToNewRepo(folders,
          repoName, false);
      results = "Files successfully committed to repo: " + repoUrl;

    } catch (Exception e) {
      results = "Error occurred in commitFromFoldersToNewRepo: " + e.getClass().getName()
          + " -  " + e.getMessage();
      throw new ProcessException(results);
    } finally {
      deleteSourceFile(sourceZip);
      IOUtils.deleteDir(tempZipFolder);
    }

    return results;

  }
  
  private static void deleteSourceFile(File sourceZip) {
    try {
      sourceZip.delete();
    } catch (Exception e) {
      // file may not have actually been sucessfully downloaded
    }
  }

}
