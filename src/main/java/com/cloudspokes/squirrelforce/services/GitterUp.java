package com.cloudspokes.squirrelforce.services;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.cloudspokes.squirrelforce.services.IOUtils;
import com.cloudspokes.squirrelforce.services.MultiSourceCommitService;

public class GitterUp {

  public static String unzipToGit(String zipUrl, String repoName) {

    String results = "";

    MultiSourceCommitService commitService = new MultiSourceCommitService();
    commitService.setCredentials(System.getenv("GIT_USERNAME"),
        System.getenv("GIT_PASSWORD"), System.getenv("GIT_OWNER"));

    File sourceZip = IOUtils.downloadFromUrlToTempFile(zipUrl);
    File overlayFolder = IOUtils.unzipToTempDir(sourceZip);
    List<File> folders = Collections.singletonList(overlayFolder);

    try {
      String repoUrl = commitService.commitFromFoldersToNewRepo(folders,
          repoName, false);
      results = "committed to repo: " + repoUrl;

    } catch (Exception e) {
      results = "commitFromFoldersToNewRepo failed: " + e.getClass().getName()
          + " -  " + e.getMessage();
    } finally {
      sourceZip.delete();
      IOUtils.deleteDir(overlayFolder);
    }

    return results;

  }

}
