/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4j.cli;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.Mdf4File;
import de.richardliebscher.mdf4.Result;
import de.richardliebscher.mdf4.datatypes.ByteArrayType;
import de.richardliebscher.mdf4.datatypes.DataType;
import de.richardliebscher.mdf4.datatypes.DataType.Visitor;
import de.richardliebscher.mdf4.datatypes.FloatType;
import de.richardliebscher.mdf4.datatypes.IntegerType;
import de.richardliebscher.mdf4.datatypes.StringType;
import de.richardliebscher.mdf4.datatypes.StructType;
import de.richardliebscher.mdf4.datatypes.UnsignedIntegerType;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.StructAccess;
import de.richardliebscher.mdf4.extract.de.UnsignedByte;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import de.richardliebscher.mdf4.extract.de.UnsignedShort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Type.Repetition;

/**
 * CLI to read and convert MDF4 files.
 */
public class Main {

  /**
   * Entrypoint.
   *
   * @param args Commandline arguments
   * @throws IOException                   I/O error
   * @throws ChannelGroupNotFoundException Channel group not found
   */
  public static void main(String[] args) throws IOException, ChannelGroupNotFoundException {
    final var sourcePath = Path.of(args[0]);
    final var targetPath = new org.apache.hadoop.fs.Path(args[1]);

    final var reader = Mdf4File.open(sourcePath);

    List<Type> fields = new ArrayList<>();
    for (Result<Channel, IOException> channelResult : reader.getDataGroups().iterator().next()
        .get().getChannelGroups().iterator().next().get().getChannels()) {
      final var channel = channelResult.get();

      final var repetition = channel.isInvalidable() ? Repetition.OPTIONAL : Repetition.REQUIRED;
      final var name = channel.getName();
      fields.add(channel.getRawDataType().accept(new Visitor<Type, IOException>() {
        @Override
        public Type visit(IntegerType type) throws IOException {
          if (type.getBitCount() == 1) {
            return new PrimitiveType(repetition, PrimitiveTypeName.BOOLEAN, name);
          } else if (type.getBitCount() <= 32) {
            return new PrimitiveType(repetition, PrimitiveTypeName.INT32, name);
          } else if (type.getBitCount() <= 64) {
            return new PrimitiveType(repetition, PrimitiveTypeName.INT64, name);
          } else {
            throw new IOException("Integer types bigger than 64 bits not supported");
          }
        }

        @Override
        public Type visit(UnsignedIntegerType type) throws IOException {
          if (type.getBitCount() == 1) {
            return new PrimitiveType(repetition, PrimitiveTypeName.BOOLEAN, name);
          } else if (type.getBitCount() <= 31) {
            return new PrimitiveType(repetition, PrimitiveTypeName.INT32, name);
          } else if (type.getBitCount() <= 63) {
            return new PrimitiveType(repetition, PrimitiveTypeName.INT64, name);
          } else {
            throw new IOException("Unsigned integer types bigger than 63 bits not supported");
          }
        }

        @Override
        public Type visit(FloatType type) throws IOException {
          if (type.getBitCount() == 16 || type.getBitCount() == 32) {
            return new PrimitiveType(repetition, PrimitiveTypeName.FLOAT, name);
          } else if (type.getBitCount() == 64) {
            return new PrimitiveType(repetition, PrimitiveTypeName.DOUBLE, name);
          } else {
            throw new IOException("Float types should have 16, 32 or 64 bits");
          }
        }

        @Override
        public Type visit(StringType type) throws RuntimeException {
          return new PrimitiveType(repetition, PrimitiveTypeName.BINARY, name);
        }

        @Override
        public Type visitElse(DataType type) throws IOException {
          throw new IOException("Unsupported data type: " + type);
        }
      }));
    }
    MessageType schema = new MessageType("Record", fields);

    try (final var writer = new Builder(targetPath).withType(schema).build()) {
      final var recordReader = reader.newRecordReader(new RecordFactory<RecordConsumer, Void>() {
        @Override
        public boolean selectGroup(DataGroup dataGroup, ChannelGroup group) {
          return true;
        }

        @Override
        public DeserializeInto<RecordConsumer> selectChannel(DataGroup dataGroup,
            ChannelGroup group, Channel channel) throws IOException {
          return createDeserializeInto(channel.getRawDataType());
        }

        @Override
        public RecordConsumer createRecordBuilder() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Void finishRecord(RecordConsumer unfinishedRecord) {
          throw new UnsupportedOperationException();
        }
      });

      while (recordReader.hasNext()) {
        writer.write(recordReader::nextInto);
      }
    }
  }

  private static DeserializeInto<RecordConsumer> createDeserializeInto(DataType dataType)
      throws IOException {
    return dataType.accept(new Visitor<DeserializeInto<RecordConsumer>, IOException>() {
      @Override
      public DeserializeInto<RecordConsumer> visit(IntegerType type)
          throws IOException {
        if (type.getBitCount() == 1) {
          return new BooleanFieldDeserialize();
        } else if (type.getBitCount() <= 32) {
          return new Int32FieldDeserialize();
        } else if (type.getBitCount() <= 64) {
          return new Int64FieldDeserialize();
        } else {
          throw new IOException("Integer types bigger than 64 bits not supported");
        }
      }

      @Override
      public DeserializeInto<RecordConsumer> visit(UnsignedIntegerType type)
          throws IOException {
        if (type.getBitCount() == 1) {
          return new BooleanFieldDeserialize();
        } else if (type.getBitCount() <= 31) {
          return new Int32FieldDeserialize();
        } else if (type.getBitCount() <= 63) {
          return new Int64FieldDeserialize();
        } else {
          throw new IOException(
              "Unsigned integer types bigger than 63 bits not supported");
        }
      }

      @Override
      public DeserializeInto<RecordConsumer> visit(FloatType type)
          throws IOException {
        if (type.getBitCount() == 16 || type.getBitCount() == 32) {
          return new Float32FieldDeserialize();
        } else if (type.getBitCount() == 64) {
          return new Float64FieldDeserialize();
        } else {
          throw new IOException("Float types should have 16, 32 or 64 bits");
        }
      }

      @Override
      public DeserializeInto<RecordConsumer> visit(StringType type)
          throws RuntimeException {
        return new StringFieldDeserialize();
      }

      @Override
      public DeserializeInto<RecordConsumer> visit(ByteArrayType type)
          throws RuntimeException {
        return new ByteArrayFieldDeserialize();
      }

      @Override
      public DeserializeInto<RecordConsumer> visit(StructType type) throws IOException {
        return new StructFieldDeserialize(type);
      }

      @Override
      public DeserializeInto<RecordConsumer> visitElse(DataType type) throws IOException {
        throw new IOException("Unsupported data type " + type);
      }
    });
  }


  static class Builder extends ParquetWriter.Builder<Record, Builder> {

    private MessageType type = null;

    private Builder(org.apache.hadoop.fs.Path file) {
      super(file);
    }

    public Builder withType(MessageType type) {
      this.type = type;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    protected WriteSupport<Record> getWriteSupport(Configuration conf) {
      return new RecordWriteSupport(type);
    }
  }

  @RequiredArgsConstructor
  static class RecordWriteSupport extends WriteSupport<Record> {

    private final MessageType schema;
    private RecordConsumer recordConsumer;

    @Override
    public WriteContext init(Configuration configuration) {
      return new WriteContext(schema, Map.of());
    }

    @Override
    public void prepareForWrite(RecordConsumer recordConsumer) {
      this.recordConsumer = recordConsumer;
    }

    @Override
    public void write(Record record) {
      recordConsumer.startMessage();
      try {
        record.writeInto(recordConsumer);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      recordConsumer.endMessage();
    }
  }

  interface Record {

    void writeInto(RecordConsumer recordConsumer) throws IOException;
  }

  @RequiredArgsConstructor
  @Getter
  static final class Int32FieldDeserialize implements DeserializeInto<RecordConsumer>,
      de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

    @Override
    public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
        throws IOException {
      deserializer.deserialize_value(this, recordConsumer);
    }

    @Override
    public String expecting() {
      return "32-bit integer";
    }

    @Override
    public Void visitU8(byte value, RecordConsumer recordConsumer) {
      return visitI32(UnsignedByte.toInt(value), recordConsumer);
    }

    @Override
    public Void visitU16(short value, RecordConsumer recordConsumer) {
      return visitI32(UnsignedShort.toInt(value), recordConsumer);
    }

    @Override
    public Void visitU32(int value, RecordConsumer recordConsumer) {
      return visitI32(Math.toIntExact(UnsignedInteger.toLong(value)), recordConsumer);
    }

    @Override
    public Void visitI32(int value, RecordConsumer recordConsumer) {
      recordConsumer.addInteger(value);
      return null;
    }

    @Override
    public Void visitInvalid(RecordConsumer recordConsumer) {
      return null;
    }
  }

  @RequiredArgsConstructor
  @Getter
  static final class BooleanFieldDeserialize implements DeserializeInto<RecordConsumer>,
      de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

    @Override
    public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
        throws IOException {
      deserializer.deserialize_value(this, recordConsumer);
    }

    @Override
    public String expecting() {
      return "boolean";
    }

    @Override
    public Void visitU8(byte value, RecordConsumer recordConsumer) {
      recordConsumer.addBoolean(value != 0);
      return null;
    }

    @Override
    public Void visitI8(byte value, RecordConsumer recordConsumer) {
      recordConsumer.addBoolean(value != 0);
      return null;
    }

    @Override
    public Void visitInvalid(RecordConsumer recordConsumer) {
      return null;
    }
  }

  @RequiredArgsConstructor
  @Getter
  static final class Int64FieldDeserialize implements DeserializeInto<RecordConsumer>,
      de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

    @Override
    public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
        throws IOException {
      deserializer.deserialize_value(this, recordConsumer);
    }

    @Override
    public String expecting() {
      return "64-bit integer";
    }

    @Override
    public Void visitU32(int value, RecordConsumer recordConsumer) {
      return visitI64(UnsignedInteger.toLong(value), recordConsumer);
    }

    @Override
    public Void visitU64(long value, RecordConsumer recordConsumer) {
      if (value < 0) {
        throw new ArithmeticException("long overflow"); // TODO
      }
      return visitI64(value, recordConsumer);
    }

    @Override
    public Void visitI64(long value, RecordConsumer recordConsumer) {
      recordConsumer.addLong(value);
      return null;
    }

    @Override
    public Void visitInvalid(RecordConsumer recordConsumer) {
      return null;
    }
  }

  @RequiredArgsConstructor
  @Getter
  static final class Float32FieldDeserialize implements DeserializeInto<RecordConsumer>,
      de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

    @Override
    public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
        throws IOException {
      deserializer.deserialize_value(this, recordConsumer);
    }

    @Override
    public String expecting() {
      return "32-bit float";
    }

    @Override
    public Void visitF32(float value, RecordConsumer recordConsumer) {
      recordConsumer.addFloat(value);
      return null;
    }

    @Override
    public Void visitInvalid(RecordConsumer recordConsumer) {
      return null;
    }
  }

  @RequiredArgsConstructor
  @Getter
  static final class Float64FieldDeserialize implements DeserializeInto<RecordConsumer>,
      de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

    @Override
    public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
        throws IOException {
      deserializer.deserialize_value(this, recordConsumer);
    }

    @Override
    public String expecting() {
      return "64-bit float";
    }

    @Override
    public Void visitF64(double value, RecordConsumer recordConsumer) {
      recordConsumer.addDouble(value);
      return null;
    }

    @Override
    public Void visitInvalid(RecordConsumer recordConsumer) {
      return null;
    }
  }

  @RequiredArgsConstructor
  @Getter
  static final class StringFieldDeserialize implements DeserializeInto<RecordConsumer>,
      de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

    @Override
    public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
        throws IOException {
      deserializer.deserialize_value(this, recordConsumer);
    }

    @Override
    public String expecting() {
      return "string";
    }

    @Override
    public Void visitString(String value, RecordConsumer recordConsumer) {
      recordConsumer.addBinary(Binary.fromString(value));
      return null;
    }

    @Override
    public Void visitInvalid(RecordConsumer recordConsumer) {
      return null;
    }
  }

  @RequiredArgsConstructor
  @Getter
  static final class ByteArrayFieldDeserialize implements DeserializeInto<RecordConsumer>,
      de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

    @Override
    public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
        throws IOException {
      deserializer.deserialize_value(this, recordConsumer);
    }

    @Override
    public String expecting() {
      return "byte array";
    }

    @Override
    public Void visitByteArray(ByteBuffer bytes, RecordConsumer recordConsumer) {
      recordConsumer.addBinary(Binary.fromReusedByteBuffer(bytes));
      return null;
    }

    @Override
    public Void visitByteArray(byte[] bytes, int offset, int length,
        RecordConsumer recordConsumer) {
      recordConsumer.addBinary(Binary.fromReusedByteArray(bytes, offset, length));
      return null;
    }

    @Override
    public Void visitByteArray(byte[] bytes, RecordConsumer recordConsumer) {
      recordConsumer.addBinary(Binary.fromReusedByteArray(bytes));
      return null;
    }

    @Override
    public Void visitInvalid(RecordConsumer recordConsumer) {
      return null;
    }
  }

  @RequiredArgsConstructor
  @Getter
  static final class StructFieldDeserialize implements DeserializeInto<RecordConsumer>,
      de.richardliebscher.mdf4.extract.de.Visitor<Void, RecordConsumer> {

    private final String[] fieldNames;
    private final DeserializeInto<RecordConsumer>[] fieldDe;

    @SuppressWarnings("unchecked")
    public StructFieldDeserialize(StructType type) throws IOException {
      final var fieldCount = type.fields().size();
      fieldNames = new String[fieldCount];
      fieldDe = (DeserializeInto<RecordConsumer>[]) new DeserializeInto<?>[fieldCount];

      final var fields = type.fields();
      for (int i = 0; i < fieldCount; i++) {
        final var field = fields.get(i);
        fieldNames[i] = field.getKey();
        fieldDe[i] = createDeserializeInto(field.getValue());
      }
    }

    @Override
    public void deserializeInto(Deserializer deserializer, RecordConsumer recordConsumer)
        throws IOException {
      deserializer.deserialize_value(this, recordConsumer);
    }

    @Override
    public String expecting() {
      return "structure";
    }

    @Override
    public Void visitStruct(StructAccess struct, RecordConsumer recordConsumer) throws IOException {
      final var fields = struct.fields();
      recordConsumer.startGroup();
      for (int i = 0; i < fields; i++) {
        recordConsumer.startField(fieldNames[i], i);
        struct.next_field(fieldDe[i], recordConsumer);
        recordConsumer.endField(fieldNames[i], i);
      }
      recordConsumer.endGroup();
      return null;
    }

    @Override
    public Void visitInvalid(RecordConsumer recordConsumer) {
      return null;
    }
  }
}
