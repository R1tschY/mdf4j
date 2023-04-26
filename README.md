# mdf4j
*MDF4 Reader for Java*

## Goal

* Reading channel data using Java

## Supported Blocks

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

### Java

```java
import java.nio.file.Files;
import java.nio.file.Path;
import de.richardliebscher.mdf4.blocks.Channel;
import de.richardliebscher.mdf4.blocks.ChannelGroup;
import de.richardliebscher.mdf4.blocks.DataGroup;
import de.richardliebscher.mdf4.extract.ChannelSelector;
import de.richardliebscher.mdf4.extract.de.ObjectDeserialize;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.io.ByteBufferInput;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Example {
    public static void main(String[] args) throws Exception {
        // Open file
        final var mdf4File = Mdf4File.open(
                new ByteBufferInput(ByteBuffer.wrap(Files.readAllBytes(Path.of("example.mf4")))));

        // Specify what channels to read
        final var channelSelector = new ChannelSelector() {
            @Override
            public boolean selectGroup(DataGroup dg, ChannelGroup group) {
                // Select first data group
                return true;
            }
            
            @Override
            public boolean selectChannel(DataGroup dg, ChannelGroup group, Channel channel) {
                // Select all supported channels
                return true;
            }
        };

        // Deserialize records in an arbitrary format
        final var recordVisitor = new RecordVisitor<List<Object>>() {
            @Override
            public List<Object> visitRecord(RecordAccess rowAccess) {
                final var de = new ObjectDeserialize();

                List<Object> objects = new ArrayList<>();
                Object elem;
                while ((elem = rowAccess.nextElement(de)) != null) {
                    objects.add(elem);
                }
                return objects;
            }
        };
        
        // Start the reading
        final var recordReader = mdf4File.newRecordReader(channelSelector, recordVisitor);
        List<Object> row;
        while ((row = recordReader.next()) != null) {
            System.out.println(row);
        }
    }

}
```