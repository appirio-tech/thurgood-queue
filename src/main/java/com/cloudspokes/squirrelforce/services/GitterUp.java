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

    File sourceZip = IOUtils.downloadFromUrlToTempFile(zipUrl);
    File tempZipFolder = IOUtils.unzipToTempDir(sourceZip);
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
      sourceZip.delete();
      IOUtils.deleteDir(tempZipFolder);
    }

    return results;

  }

}
