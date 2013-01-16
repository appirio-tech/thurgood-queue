package com.cloudspokes.squirrelforce;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

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
          System.out.println(System.getenv("GIT_USERNAME"));
          System.out.println(System.getenv("GIT_PASSWORD"));
          System.out.println(System.getenv("GIT_OWNER"));
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
    System.out.println("99. Exit");
    System.out.println(" ");
    System.out.println("Operation: ");
  }

}
