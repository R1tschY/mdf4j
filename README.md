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
* Synchronization channel
* Non-byte-aligned integer
* Integer bit sizes apart from 8, 16, 32, 64
* 16-bit/half floating point value
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
    implementation('de.richardliebscher.mdf4j:mdf4j:0.1.0')
}
```

### Maven
```xml
<dependency>
  <groupId>de.richardliebscher.mdf4j</groupId>
  <artifactId>mdf4j</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Examples

Look at [src/test/java/de/richardliebscher/mdf4/examples](src/test/java/de/richardliebscher/mdf4/examples).

