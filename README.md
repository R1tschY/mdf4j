# mdf4j
*[ASAM MDF4](https://www.asam.net/standards/detail/mdf/) Reader for Java*

## Goals

* Reading channel data using Java
* Do lazy loading as much as possible to reduce initial loading time

## Supported

* Blocks
  * CC
  * CG
  * CN
  * DG
  * DL
  * DT
  * DZ
  * HD
  * HL
  * ID
  * SI
* Versions
  * \>=4.00,<=4.20 Supported
  * \>4.20,<5.00 Supported when no new features for reading are needed

## Unsupported
* MLC (maximum length channel)
* VLDC (variable length data channel)
* Non-byte-aligned integer
* Integer bit sizes bigger than 64
* Unsupported data types:
  * CANOpen Date
  * CANOpen Time
  * Complex
  * MIME Sample/Stream
  * Structure
  * Array
* Conversion apart from Identity and Linear
* Unsorted channel data
* Unfinished files
* Events
* Bus logging
* Column storage

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

