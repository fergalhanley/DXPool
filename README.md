# DXPool

A dynamic, extensible and extremely lightweight object pool library that uses lambda expressions and generics.

## Installation

Requires Java 8 or later.


```xml
<dependency>
  <groupId>com.github.fergalhanley</groupId>
  <artifactId>dxpool</artifactId>
  <version>1.0</version>
</dependency>
```

## Examples

To create a new pool we do three things:
  1. Give it a name,
  2. Specify an initializer function that returns the object to be added to the pool,
  3. Optionally, specify a function to be called when the object is being destroyed and removed from the pool.

```java
DXPool.create("my-connection-pool")
      .initialize(() -> DriverManager.getConnection( JDBC_CONNECTION_STRING ))
      .destroy( o -> ((Connection)o).close() );
```

To use an object from the pool we call the pool by name and execute a meth

```java
DXPool.with("my-connection-pool")
      .execute( o -> {
          // cast to create the object
          Connection conn = ((Connection)o);
      });
```

You can also specify methods to run before and after executing on an pool object instance.

```java
DXPool.create("my-connection-pool")
      .initialize( () -> {
        :
        return myObject;
      })
      .before( myObject -> {
        System.out.println("Just about to use " + myObject.toString());
      })
      .after( myObject -> {
        System.out.println("Just used " + myObject.toString());
      })
      .destroy( myObject -> {
        :
        :
      });
```
