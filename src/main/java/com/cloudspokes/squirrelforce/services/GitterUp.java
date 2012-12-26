package com.cloudspokes.squirrelforce.services;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.cloudspokes.squirrelforce.VirtualFile;

public class GitterUp {
  
  private static final String APEX_SHELL_PATH   = "WEB-INF/shells/apex";

  public static String unzipToGit(String zipUrl, String repoName) {

    String results = "";

    MultiSourceCommitService commitService = new MultiSourceCommitService();
    commitService.setCredentials(System.getenv("GIT_USERNAME"),
        System.getenv("GIT_PASSWORD"), System.getenv("GIT_OWNER"));
    
    File apexShellFolder = VirtualFile.fromRelativePath(APEX_SHELL_PATH);  

    File sourceZip = IOUtils.downloadFromUrlToTempFile(zipUrl);
    File tempZipFolder = IOUtils.unzipToTempDir(sourceZip);
    //List<File> folders = Collections.singletonList(tempZipFolder);
    List<File> folders = Arrays.asList(apexShellFolder, tempZipFolder);

    try {
      String repoUrl = commitService.commitFromFoldersToNewRepo(folders,
          repoName, false);
      results = "committed to repo: " + repoUrl;

    } catch (Exception e) {
      results = "commitFromFoldersToNewRepo failed: " + e.getClass().getName()
          + " -  " + e.getMessage();
    } finally {
      sourceZip.delete();
      IOUtils.deleteDir(tempZipFolder);
    }

    return results;

  }

}
