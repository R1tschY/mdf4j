# mdf4j
*MDF4 Reader for Java*

## Goal

* Reading channel data using Java

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

Soon available at Maven Central:

### Gradle
```groovy
dependencies {
    // ...
    implementation('de.richardliebscher.mdf4j:mdf4j:0.1.0-SNAPSHOT')
}
```

### Maven
```xml
<dependency>
  <groupId>de.richardliebscher.mdf4j</groupId>
  <artifactId>mdf4j</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Examples

Look at [src/test/java/de/richardliebscher/mdf4/examples](src/test/java/de/richardliebscher/mdf4/examples).

