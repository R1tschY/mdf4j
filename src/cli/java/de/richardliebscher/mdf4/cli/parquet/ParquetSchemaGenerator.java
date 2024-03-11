/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.datatypes.ByteArrayType;
import de.richardliebscher.mdf4.datatypes.DataType;
import de.richardliebscher.mdf4.datatypes.DataType.Visitor;
import de.richardliebscher.mdf4.datatypes.FloatType;
import de.richardliebscher.mdf4.datatypes.IntegerType;
import de.richardliebscher.mdf4.datatypes.StringType;
import de.richardliebscher.mdf4.datatypes.StructField;
import de.richardliebscher.mdf4.datatypes.StructType;
import de.richardliebscher.mdf4.datatypes.UnsignedIntegerType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Type.Repetition;

public class ParquetSchemaGenerator {

  private final DataGroup dataGroup;
  private final ChannelGroup channelGroup;
  private final List<Channel> channels;

  public ParquetSchemaGenerator(DataGroup dataGroup, ChannelGroup channelGroup,
      List<Channel> channels) {
    this.dataGroup = dataGroup;
    this.channelGroup = channelGroup;
    this.channels = channels;
  }

  public MessageType generateSchema() throws IOException {
    final var fields = new ArrayList<Type>(channels.size());
    for (Channel channel : channels) {
      fields.add(generateSchema(channel));
    }
    return new MessageType(channelGroup.getName().orElse("MDF4 file"), fields);
  }

  private static Type generateSchema(Channel channel) throws IOException {
    final var repetition = channel.isInvalidable() ? Repetition.OPTIONAL : Repetition.REQUIRED;
    final var name = channel.getName();
    return channel.getDataType().accept(new SchemaGenerator(repetition, name));
  }

  private static Type generateSchema(StructField field) throws IOException {
    final var repetition = Repetition.OPTIONAL;
    final var name = field.name();
    return field.dataType().accept(new SchemaGenerator(repetition, name));
  }

  private static class SchemaGenerator implements Visitor<Type, IOException> {
    private final Repetition repetition;
    private final String name;

    public SchemaGenerator(Repetition repetition, String name) {
      this.repetition = repetition;
      this.name = name;
    }

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
    public Type visit(StringType type) {
      return new PrimitiveType(repetition, PrimitiveTypeName.BINARY, name);
    }

    @Override
    public Type visit(ByteArrayType type) {
      return new PrimitiveType(repetition, PrimitiveTypeName.BINARY, name);
    }

    @Override
    public Type visit(StructType type) throws IOException {
      final var fields = new ArrayList<Type>(type.fields().size());
      for (StructField structField : type.fields()) {
        fields.add(generateSchema(structField));
      }
      return new GroupType(repetition, name, fields);
    }

    @Override
    public Type visitElse(DataType type) throws IOException {
      throw new IOException("Unsupported data type: " + type);
    }
  }
}
