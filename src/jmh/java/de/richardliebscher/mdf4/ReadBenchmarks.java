package de.richardliebscher.mdf4;

import static java.util.Objects.requireNonNull;

import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.SerializableRecordFactory;
import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Half;
import de.richardliebscher.mdf4.extract.de.SerializableDeserializeInto;
import de.richardliebscher.mdf4.extract.de.UnsignedByte;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.UnsignedShort;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.io.ByteBufferInput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

public class ReadBenchmarks {
  @State(Scope.Benchmark)
  public static class ExecutionPlan {
    public static final Path PATH = Path.of(
        URI.create(requireNonNull(
            ReadBenchmarks.class.getResource("/KonvektionKalt1-20140123-143636.mf4")).toString()));

    public byte[] source;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
      source = Files.readAllBytes(PATH);
    }
  }


  @Benchmark
  @Measurement(iterations = 3)
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  public String simple(ExecutionPlan state) throws Exception {
    return SimpleCsvConverter.convert(state.source);
  }

  @Benchmark
  @Measurement(iterations = 3)
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  public String simpleStream(ExecutionPlan state) throws Exception {
    return SimpleStreamCsvConverter.convert(state.source);
  }

  @Benchmark
  @Measurement(iterations = 3)
  @Warmup(iterations = 1, time = 1, batchSize = 1)
  @Fork(value = 1, warmups = 0)
  public String parallelStream(ExecutionPlan state) throws Exception {
    return ParallelStreamCsvConverter.convert(state.source);
  }
}


class SimpleCsvConverter {

  private static final String SEP = ",";
  private static final String LINE_SEP = "\n";

  public static String convert(byte[] source) throws Exception {
    // Open file
    final var input = new ByteBufferInput(ByteBuffer.wrap(source));
    final var mdf4File = Mdf4File.open(input);

    // Select channel group and channels
    final var channels = new ArrayList<Channel>();

    final var target = new StringWriter();
    final var de = new CsvColumnDeserialize();
    try (var writer = new BufferedWriter(target)) {
      final var reader = mdf4File.newRecordReader(new RecordFactory<Writer, Void>() {
        @Override
        public boolean selectGroup(DataGroup dg, ChannelGroup group) {
          // Select first channel group
          return true;
        }

        @Override
        public DeserializeInto<Writer> selectChannel(DataGroup dg, ChannelGroup group, Channel channel1) {
          channels.add(channel1);
          if (channels.size() == 1) {
            return (deserializer, writer1) -> writer1.write(de.deserialize(deserializer));
          } else {
            return (deserializer, writer1) -> {
              writer1.write(SEP);
              writer1.write(de.deserialize(deserializer));
            };
          }
        }

        @Override
        public Writer createRecordBuilder() {
          return writer;
        }

        @Override
        public Void finishRecord(Writer writer1) throws IOException {
          writer1.write(LINE_SEP);
          return null;
        }
      });

      // Write header
      boolean firstColumn = true;
      for (Channel channel : channels) {
        if (firstColumn) {
          firstColumn = false;
        } else {
          writer.write(SEP);
        }

        writer.write(channel.getName());
      }
      writer.write(LINE_SEP);

      // Write units
      firstColumn = true;
      for (Channel channel : channels) {
        if (firstColumn) {
          firstColumn = false;
        } else {
          writer.write(SEP);
        }

        writer.write(channel.getPhysicalUnit().orElse(""));
      }
      writer.write(LINE_SEP);

      // Write values
      for (int i = 0; i < reader.size(); i++) {
        reader.next();
      }
    }
    return target.toString();
  }
}

class SimpleStreamCsvConverter {

  private static final String SEP = ",";
  private static final String LINE_SEP = "\n";

  public static String convert(byte[] source) throws Exception {
    // Open file
    final var input = new ByteBufferInput(ByteBuffer.wrap(source));
    final var mdf4File = Mdf4File.open(input);

    // Select channel group and channels
    final var channels = new ArrayList<Channel>();
    final var target = new StringWriter();
    final var de = new CsvColumnDeserialize();
    try (var writer = new BufferedWriter(target)) {
      final var reader = mdf4File.streamRecords(new SerializableRecordFactory<StringBuilder, String>() {
        @Override
        public boolean selectGroup(DataGroup dg, ChannelGroup group) {
          // Select first channel group
          return true;
        }

        @Override
        public SerializableDeserializeInto<StringBuilder> selectChannel(DataGroup dg, ChannelGroup group, Channel channel1) {
          channels.add(channel1);
          if (channels.size() == 1) {
            return (deserializer, stringBuilder) -> stringBuilder.append(de.deserialize(deserializer));
          } else {
            return (deserializer, stringBuilder) -> {
              stringBuilder.append(SEP);
              stringBuilder.append(de.deserialize(deserializer));
            };
          }
        }

        @Override
        public StringBuilder createRecordBuilder() {
          return new StringBuilder();
        }

        @Override
        public String finishRecord(StringBuilder stringBuilder) {
          stringBuilder.append(LINE_SEP);
          return stringBuilder.toString();
        }
      });

      // Write header
      boolean firstColumn = true;
      for (Channel channel : channels) {
        if (firstColumn) {
          firstColumn = false;
        } else {
          writer.write(SEP);
        }

        writer.write(channel.getName());
      }
      writer.write(LINE_SEP);

      // Write units
      firstColumn = true;
      for (Channel channel : channels) {
        if (firstColumn) {
          firstColumn = false;
        } else {
          writer.write(SEP);
        }

        writer.write(channel.getPhysicalUnit().orElse(""));
      }
      writer.write(LINE_SEP);

      // Write values
      reader.forEachOrdered(line -> {
        try {
          writer.write(line.get());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    }
    return target.toString();
  }
}

class ParallelStreamCsvConverter {

  private static final String SEP = ",";
  private static final String LINE_SEP = "\n";

  public static String convert(byte[] source) throws Exception {
    // Open file
    final var input = new ByteBufferInput(ByteBuffer.wrap(source));
    final var mdf4File = Mdf4File.open(input);

    // Select channel group and channels
    final var channels = new ArrayList<Channel>();
    final var target = new StringWriter();
    final var de = new CsvColumnDeserialize();
    try (var writer = new BufferedWriter(target)) {
      final var reader = mdf4File.streamRecords(new SerializableRecordFactory<StringBuilder, String>() {
        @Override
        public boolean selectGroup(DataGroup dg, ChannelGroup group) {
          // Select first channel group
          return true;
        }

        @Override
        public SerializableDeserializeInto<StringBuilder> selectChannel(DataGroup dg, ChannelGroup group, Channel channel1) {
          channels.add(channel1);
          if (channels.size() == 1) {
            return (deserializer, stringBuilder) -> stringBuilder.append(de.deserialize(deserializer));
          } else {
            return (deserializer, stringBuilder) -> {
              stringBuilder.append(SEP);
              stringBuilder.append(de.deserialize(deserializer));
            };
          }
        }

        @Override
        public StringBuilder createRecordBuilder() {
          return new StringBuilder();
        }

        @Override
        public String finishRecord(StringBuilder stringBuilder) {
          stringBuilder.append(LINE_SEP);
          return stringBuilder.toString();
        }
      });

      // Write header
      boolean firstColumn = true;
      for (Channel channel : channels) {
        if (firstColumn) {
          firstColumn = false;
        } else {
          writer.write(SEP);
        }

        writer.write(channel.getName());
      }
      writer.write(LINE_SEP);

      // Write units
      firstColumn = true;
      for (Channel channel : channels) {
        if (firstColumn) {
          firstColumn = false;
        } else {
          writer.write(SEP);
        }

        writer.write(channel.getPhysicalUnit().orElse(""));
      }
      writer.write(LINE_SEP);

      // Write values
      reader.parallel().forEachOrdered(line -> {
        try {
          writer.write(line.get());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    }
    return target.toString();
  }
}


class CsvColumnDeserialize implements Deserialize<String> {

  @Override
  public String deserialize(Deserializer deserializer) throws IOException {
    return deserializer.deserialize_value(new Visitor<>() {
      @Override
      public String expecting() {
        return "CSV value";
      }

      @Override
      public String visitU8(byte value, Void unused) {
        return UnsignedByte.toString(value);
      }

      @Override
      public String visitU16(short value, Void unused) {
        return UnsignedShort.toString(value);
      }

      @Override
      public String visitU32(int value, Void unused) {
        return UnsignedInteger.toString(value);
      }

      @Override
      public String visitU64(long value, Void unused) {
        return UnsignedLong.toString(value);
      }

      @Override
      public String visitI8(byte value, Void unused) {
        return Byte.toString(value);
      }

      @Override
      public String visitI16(short value, Void unused) {
        return Short.toString(value);
      }

      @Override
      public String visitI32(int value, Void unused) {
        return Integer.toString(value);
      }

      @Override
      public String visitI64(long value, Void unused) {
        return Long.toString(value);
      }

      @Override
      public String visitF16(short value, Void unused) {
        return Half.toString(value);
      }

      @Override
      public String visitF32(float value, Void unused) {
        return Float.toString(value);
      }

      @Override
      public String visitF64(double value, Void unused) {
        return Double.toString(value);
      }

      @Override
      public String visitInvalid(Void unused) {
        return "";
      }
    }, (Void) null);
  }
}

