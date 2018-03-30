package com.example.circular.jdbc;

import com.google.auto.service.AutoService;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

/** A sample driver which has the same problem as the Sybase driver. */
@AutoService(Driver.class) // Create the appropriate ServiceLoader metadata
public class CircularDriver implements Driver {

  static {
    new CircularDriver();
  }

  public CircularDriver() {
    registerWithDriverManager();
  }

  protected void registerWithDriverManager() {
    System.out.println("Calling registerWithDriverManager() for " + this);
    try {
      synchronized (DriverManager.class) {
        // This part is fine. JDBC drivers are supposed to register themselves.
        DriverManager.registerDriver(this);

        // This is the problem. getDrivers() invokes ServiceLoader.load(Driver.class) which tries
        // to instantiate all of the discovered drivers. That ends up instantiating a new
        // CircularDriver, which calls this again, until a StackOverflowError breaks the
        // recursive loop.
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
          Driver otherDriver = drivers.nextElement();
          if (((otherDriver instanceof CircularDriver)) && (otherDriver != this)) {
            DriverManager.deregisterDriver(otherDriver);
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    return null;
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return false;
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    return new DriverPropertyInfo[0];
  }

  @Override
  public int getMajorVersion() {
    return 0;
  }

  @Override
  public int getMinorVersion() {
    return 0;
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return null;
  }
}
