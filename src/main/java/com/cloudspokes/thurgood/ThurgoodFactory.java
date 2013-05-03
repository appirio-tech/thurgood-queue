package com.cloudspokes.thurgood;

public class ThurgoodFactory {
  
  public Thurgood getTheJudge(String type) {
    Thurgood t = null;
    type = type.toLowerCase();
    if (type.equals("apex")) {
      t =  new Apex();
    } else if (type.equals("java")) {
      t = new Java();
    }
    t.submissionType = type;
    return t;
  }

}
