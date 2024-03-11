/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.cli.parquet.de;

import de.richardliebscher.mdf4.datatypes.ByteArrayType;
import de.richardliebscher.mdf4.datatypes.DataType;
import de.richardliebscher.mdf4.datatypes.DataType.Visitor;
import de.richardliebscher.mdf4.datatypes.FloatType;
import de.richardliebscher.mdf4.datatypes.IntegerType;
import de.richardliebscher.mdf4.datatypes.StringType;
import de.richardliebscher.mdf4.datatypes.StructType;
import de.richardliebscher.mdf4.datatypes.UnsignedIntegerType;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import java.io.IOException;
import org.apache.parquet.io.api.RecordConsumer;

public final class DeserializeIntoParquet {
  public static DeserializeInto<RecordConsumer> forType(DataType dataType) throws IOException {
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
}
