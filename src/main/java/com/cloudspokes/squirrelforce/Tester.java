package com.cloudspokes.squirrelforce;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.cloudspokes.squirrelforce.services.GitterUp;
import com.cloudspokes.squirrelforce.services.IOUtils;
import com.cloudspokes.squirrelforce.services.MultiSourceCommitService;

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
                  "http://cs-production.s3.amazonaws.com/challenges/1884/wcheung/octavius3.zip",
                  "sqtest2");

          System.out.println(results);
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
    System.out.println("\n1. First selection");
    System.out.println("99. Exit");
    System.out.println(" ");
    System.out.println("Operation: ");
  }

}
