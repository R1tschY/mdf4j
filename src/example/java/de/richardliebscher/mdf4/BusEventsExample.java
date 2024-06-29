package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.datatypes.StructType;
import de.richardliebscher.mdf4.extract.ChannelDeFactory;
import de.richardliebscher.mdf4.extract.SizedRecordReader;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.StructAccess;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.extract.de.utils.DeserializeIntoByteArrayProperty;
import de.richardliebscher.mdf4.extract.de.utils.DeserializeIntoVoid;
import de.richardliebscher.mdf4.extract.de.utils.WriteDoubleProperty;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

public class BusEventsExample {

  public static void main(String[] args) throws Exception {
    final var source = Path.of(args[0]);

    try (final var mdf4File = Mdf4File.open(source)) {
      int channelGroup = 0;
      final var readers = new ArrayList<SizedRecordReader<CanDataFrame, CanDataFrame>>();
      for (final var dataGroup : mdf4File.getDataGroups()) {
        readers.add(mdf4File.newRecordReader(
            ++channelGroup, new BusEventDeFactory(), CanDataFrame::new));
      }

      final var queue = new PriorityQueue<PendingEvent>(
          Comparator.comparing(x -> x.nextEvent.getTimestamp()));
      for (final var reader : readers) {
        if (reader.hasNext()) {
          queue.offer(new PendingEvent(reader.next(), reader));
        }
      }

      while (!queue.isEmpty()) {
        final var reader = queue.poll();
        System.out.println("Do something with " + reader.nextEvent);

        final var recordReader = reader.recordReader;
        if (recordReader.hasNext()) {
          queue.offer(new PendingEvent(recordReader.next(), recordReader));
        }
      }
    }
  }
}

@Data
@AllArgsConstructor
class PendingEvent {
  BusEvent nextEvent;
  SizedRecordReader<CanDataFrame, CanDataFrame> recordReader;
}


interface BusEvent {
  double getTimestamp();
  void setTimestamp(double value);
}

@Data
class CanDataFrame implements BusEvent {
  double timestamp;
  int id;
  byte[] dataBytes;
}

@RequiredArgsConstructor
class BusEventDeFactory implements ChannelDeFactory<CanDataFrame> {
  @Override
  public DeserializeInto<CanDataFrame> createDeserialization(
      DataGroup dg, ChannelGroup group, Channel channel) throws IOException {
    if (channel.isTimeMaster()) {
      return new TimestampDeserialize<>(BusEvent::setTimestamp);
    }
    if (!channel.containsBusEvent()) {
      return null;
    }
    if (!(channel.getDataType() instanceof StructType)) {
      return null;
    }
    final var structType = (StructType) channel.getDataType();
    if (channel.getName().equals("CAN_DataFrame")) {
      final var fieldDe = structType.fields().stream().map(field -> {
        switch (field.name()) {
          case "CAN_DataFrame.ID":
            return new CanIdDeserialize();
          case "CAN_DataFrame.DataBytes":
            return new DeserializeIntoByteArrayProperty<>(CanDataFrame::setDataBytes);
          default:
            return new DeserializeIntoVoid<CanDataFrame>();
        }
      }).collect(Collectors.toList());
      return new CanDataFrameDeserialize(fieldDe);
    }
    return null;
  }
}

@RequiredArgsConstructor
class CanDataFrameDeserialize implements DeserializeInto<CanDataFrame> {

  private final List<DeserializeInto<CanDataFrame>> fieldDe;

  @Override
  public void deserializeInto(Deserializer deserializer, CanDataFrame target)
      throws IOException {
    deserializer.deserialize_value(new Visitor<Void, CanDataFrame>() {
      @Override
      public String expecting() {
        return "CAN DataFrame";
      }

      @Override
      public Void visitStruct(StructAccess access, CanDataFrame param) throws IOException {
        for (final var de : fieldDe) {
          access.next_field(de, param);
        }
        return null;
      }
    }, target);
  }
}

@RequiredArgsConstructor
class TimestampDeserialize<T> implements DeserializeInto<T> {
  private final WriteDoubleProperty<T> setter;

  @Override
  public void deserializeInto(Deserializer deserializer, T dest) throws IOException {
    deserializer.deserialize_value(new Visitor<Void, T>() {
      @Override
      public String expecting() {
        return "Timestamp";
      }

      @Override
      public Void visitI64(long value, T param) {
        setter.writeDouble(param, value);
        return null;
      }

      @Override
      public Void visitU64(long value, T param) {
        setter.writeDouble(param, UnsignedLong.toDoubleValue(value));
        return null;
      }

      @Override
      public Void visitF64(double value, T param) {
        setter.writeDouble(param, value);
        return null;
      }
    }, dest);
  }
}

class CanIdDeserialize implements DeserializeInto<CanDataFrame> {
  @Override
  public void deserializeInto(Deserializer deserializer, CanDataFrame dest) throws IOException {
    deserializer.deserialize_value(new Visitor<Void, CanDataFrame>() {
      @Override
      public String expecting() {
        return "CAN ID";
      }

      @Override
      public Void visitI32(int value, CanDataFrame param) {
        param.setId(value);
        return null;
      }

      @Override
      public Void visitU32(int value, CanDataFrame param) {
        param.setId(Math.toIntExact(UnsignedInteger.toLong(value)));
        return null;
      }
    }, dest);
  }
}