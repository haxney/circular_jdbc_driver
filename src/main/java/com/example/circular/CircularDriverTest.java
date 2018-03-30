package com.example.circular;

import static net.bytebuddy.matcher.ElementMatchers.named;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.stream.Collectors;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;

/**
 * Illustrates the problem present in Sybase's driver
 */
public class CircularDriverTest {

    private static final String DRIVER_NAME = "com.example.circular.jdbc.CircularDriver";

  public static class SkipAdvice {
    @Advice.OnMethodEnter(skipOn = SkipAdvice.class)
    private static Object enter() {
      return new SkipAdvice();
    }
  }

  @SuppressWarnings("unchecked")
  private static Class<Driver> getUsingByteBuddy() {
    System.out.print("Overriding driver with ByteBuddy...");
    TypePool typePool = TypePool.Default.ofClassPath();
    TypeDescription driverDesc = typePool.describe(DRIVER_NAME).resolve();

    return (Class<Driver>)
        new ByteBuddy()
            .redefine(driverDesc, ClassFileLocator.ForClassLoader.ofClassPath())
            .visit(Advice.to(SkipAdvice.class).on(named("registerWithDriverManager")))
            .make()
            .load(ClassLoader.getSystemClassLoader())
            .getLoaded();
  }

  @SuppressWarnings("unchecked")
  private static Class<Driver> getUsingClassForName() throws ClassNotFoundException {
    System.out.println("Loading driver with Class.forName()");
    return (Class<Driver>) Class.forName(DRIVER_NAME);
  }

  public static void main(String[] args)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
          InstantiationException, SQLException, ClassNotFoundException {
    System.out.println("Running with Java version: " + System.getProperty("java.version"));
    System.out.print("Setting DriverManager logWriter...");
    DriverManager.setLogWriter(new PrintWriter(System.out));
    System.out.println("done");

    Class<Driver> driverClass;
    if (System.getProperty("useByteBuddy", "true").equals("true")) {
      driverClass = getUsingByteBuddy();
    } else {
      driverClass = getUsingClassForName();
    }

    System.out.println("done");
    System.out.println("Driver class:" + driverClass);

    System.out.println(
        "Running DriverManager.getDrivers() after loading only: "
            + DriverManager.drivers().collect(Collectors.toList()));

    System.out.println("Registering driver explicitly");
    DriverManager.registerDriver(driverClass.getDeclaredConstructor().newInstance());
    System.out.println("Done driver explicitly");

    System.out.println(
        "Running DriverManager.getDrivers() after explicit register"
            + DriverManager.drivers().collect(Collectors.toList()));
  }
}
