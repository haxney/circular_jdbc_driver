# Demonstration of circular driver loading

This repo is a demonstration of the problem that occurs with the Sybase JDBC
driver when running under Java 9. The Sybase driver is closed source and
can't be redistributed, so this includes a simple implementation of
`java.sql.Driver` for illustration.

## The problem

The dummy driver `CircularDriver` has a static initializer which instantiates a
new `CircularDriver`. The `CircularDriver` constructor calls
`registerWithDriverManager()`, which iterates through the drivers available to
`DriverManager` and tries to remove any other `CircularDriver` instances that
are not the current instance.

The problem is that, in Java 9, `DriverManager` uses `ServiceLoader` to load and
instantiate drivers. So the constructor of `CircularDriver` calls
`DriverManager.getDrivers()`, which uses `ServiceLoader` to find all drivers,
which instantiates a new `CircularDriver`, which calls back to
`DriverManager.getDrivers()`, and so on.

## Solution

In normal circumstances, we can't easily modify the Sybase driver directly, so
the solution is to use [ByteBuddy](http://bytebuddy.net) to rewrite the class at
runtime to skip `registerWithDriverManager()`. This prevents the loop of
loading.

## Running the example

The test runner can run with ByteBuddy off, to illustrate the problem, or with
ByteBuddy on to demonstrate that the solution works. Since the test has to do
with class loading, trying to run both versions in the same JVM could cause them
to interfere with each other. After `CircularDriver` is loaded, even without the
fix, things will work normally.

First, compile the code:

    mvn compile

Then run the sample as:

    mvn exec:exec -DuseByteBuddy=true

to run with ByteBuddy (the default) or

    mvn exec:exec -DuseByteBuddy=false

to run without the ByteBuddy fix.

## What to look for

The "failing" case will produce hundreds of lines like this:

    registerDriver: com.example.circular.jdbc.CircularDriver@1bce4f0a
    Calling registerWithDriverManager() for com.example.circular.jdbc.CircularDriver@5e3a8624

as well as

    DriverManager.initialize: jdbc.drivers = null
    JDBC DriverManager initialized

Make sure you are running under Java 9 if `-DuseByteBuddy=false` does not
produce the error output.
