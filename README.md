# mdf4j
*[ASAM MDF4](https://www.asam.net/standards/detail/mdf/) Reader for Java*

## Goals

* Reading channels using pure Java
* Do lazy loading as much as possible to reduce initial loading time

## Supported

* Reading channels
  * [including bus events](src/example/java/de/richardliebscher/mdf4/BusEventsExample.java)
* Reading channel information
* Versions
  * \>=4.00,<=4.20 Supported
  * \>4.20,<5.00 Supported when no new features for reading are needed

## Unsupported
* Non-byte-aligned integer
* Integer bit sizes bigger than 64
* Unsupported data types:
  * CANOpen Date
  * CANOpen Time
  * Complex
  * MIME Sample/Stream
  * Array
* Conversion apart from Identity and Linear
* Unsorted channel data
* Unfinished files
* Events
* Column storage
* Sample reduction

## Usage

Available at Maven Central:

### Gradle
```groovy
dependencies {
    // ...
    implementation('de.richardliebscher.mdf4j:mdf4j:0.2.0')
}
```

### Maven
```xml
<dependency>
  <groupId>de.richardliebscher.mdf4j</groupId>
  <artifactId>mdf4j</artifactId>
  <version>0.2.0</version>
</dependency>
```

### Examples

For examples for how to use this library, look at [src/example/java/de/richardliebscher/mdf4](src/example/java/de/richardliebscher/mdf4).

