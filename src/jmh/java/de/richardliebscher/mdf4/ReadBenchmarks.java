package de.richardliebscher.mdf4;

import static java.util.Objects.requireNonNull;

import de.richardliebscher.mdf4.extract.ChannelSelector;
import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Half;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.extract.de.SerializableRecordVisitor;
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
    final var channelSelector = new ChannelSelector() {
      @Override
      public boolean selectChannel(DataGroup dg, ChannelGroup group, Channel channel) {
        channels.add(channel);
        return true;
      }

      @Override
      public boolean selectGroup(DataGroup dg, ChannelGroup group) {
        return true;
      }
    };

    final var target = new StringWriter();
    try (var writer = new BufferedWriter(target)) {
      final var recordDe = new RecordVisitor<>() {
        @Override
        public Object visitRecord(RecordAccess recordAccess) throws IOException {
          final var de = new CsvColumnDeserialize();

          while (recordAccess.remaining() > 0) {
            writer.write(recordAccess.nextElement(de));
            if (recordAccess.remaining() > 1) {
              writer.write(SEP);
            }
          }

          writer.write(LINE_SEP);
          return null;
        }
      };

      final var reader = mdf4File.newRecordReader(channelSelector, recordDe);

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
    final var channelSelector = new ChannelSelector() {
      @Override
      public boolean selectChannel(DataGroup dg, ChannelGroup group, Channel channel) {
        channels.add(channel);
        return true;
      }

      @Override
      public boolean selectGroup(DataGroup dg, ChannelGroup group) {
        return true;
      }
    };

    final var target = new StringWriter();
    try (var writer = new BufferedWriter(target)) {
      final var recordDe = new SerializableRecordVisitor<String>() {
        @Override
        public String visitRecord(RecordAccess recordAccess) throws IOException {
          final var de = new CsvColumnDeserialize();

          final var builder = new StringBuilder();

          while (recordAccess.remaining() > 0) {
            writer.write(recordAccess.nextElement(de));
            if (recordAccess.remaining() > 1) {
              writer.write(SEP);
            }
          }

          builder.append(LINE_SEP);
          return builder.toString();
        }
      };

      final var reader = mdf4File.streamRecords(channelSelector, recordDe);

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
    final var channelSelector = new ChannelSelector() {
      @Override
      public boolean selectChannel(DataGroup dg, ChannelGroup group, Channel channel) {
        channels.add(channel);
        return true;
      }

      @Override
      public boolean selectGroup(DataGroup dg, ChannelGroup group) {
        return true;
      }
    };

    final var target = new StringWriter();
    try (var writer = new BufferedWriter(target)) {
      final var recordDe = new SerializableRecordVisitor<String>() {
        @Override
        public String visitRecord(RecordAccess recordAccess) throws IOException {
          final var de = new CsvColumnDeserialize();

          final var builder = new StringBuilder();

          while (recordAccess.remaining() > 0) {
            writer.write(recordAccess.nextElement(de));
            if (recordAccess.remaining() > 1) {
              writer.write(SEP);
            }
          }

          builder.append(LINE_SEP);
          return builder.toString();
        }
      };

      final var reader = mdf4File.streamRecords(channelSelector, recordDe);

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
      public String visitU8(byte value) {
        return UnsignedByte.toString(value);
      }

      @Override
      public String visitU16(short value) {
        return UnsignedShort.toString(value);
      }

      @Override
      public String visitU32(int value) {
        return UnsignedInteger.toString(value);
      }

      @Override
      public String visitU64(long value) {
        return UnsignedLong.toString(value);
      }

      @Override
      public String visitI8(byte value) {
        return Byte.toString(value);
      }

      @Override
      public String visitI16(short value) {
        return Short.toString(value);
      }

      @Override
      public String visitI32(int value) {
        return Integer.toString(value);
      }

      @Override
      public String visitI64(long value) {
        return Long.toString(value);
      }

      @Override
      public String visitF16(short value) {
        return Half.toString(value);
      }

      @Override
      public String visitF32(float value) {
        return Float.toString(value);
      }

      @Override
      public String visitF64(double value) {
        return Double.toString(value);
      }

      @Override
      public String visitInvalid() {
        return "";
      }
    });
  }
}

