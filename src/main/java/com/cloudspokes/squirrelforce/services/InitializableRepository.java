package com.cloudspokes.squirrelforce.services;

import org.eclipse.egit.github.core.Repository;

@SuppressWarnings("serial")
public class InitializableRepository extends Repository {

  private boolean autoInit = true;

  public boolean isAutoInit() {
    return this.autoInit;
  }

  public Repository setAutoInit(boolean autoInit) {
    this.autoInit = autoInit;
    return this;
  }

}
