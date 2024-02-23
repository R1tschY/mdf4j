/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.examples;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.Mdf4File;
import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Half;
import de.richardliebscher.mdf4.extract.de.UnsignedByte;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.UnsignedShort;
import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * CSV converter example build for simplicity (not speed).
 */
public class CsvExample {

  private static final String SEP = ",";
  private static final String LINE_SEP = "\n";

  public static void main(String[] args) throws Exception {
    final var source = Path.of(args[0]);
    final var target = Path.of(args[1]);

    // Open file
    final var mdf4File = Mdf4File.open(source);


    // Select channel group and channels
    final var de = new CsvColumnDeserialize();
    try (var writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {

      final var reader = mdf4File.newRecordReader(new RecordFactory<Writer, Void>() {
        private int channelIndex;

        @Override
        public boolean selectGroup(DataGroup dg, ChannelGroup group) {
          // Select first channel group
          return true;
        }

        @Override
        public DeserializeInto<Writer> selectChannel(
            DataGroup dg, ChannelGroup group, Channel channel1) {
          if (channelIndex++ == 0) {
            return (deserializer, writer) -> writer.write(de.deserialize(deserializer));
          } else {
            return (deserializer, writer) -> {
              writer.write(SEP);
              writer.write(de.deserialize(deserializer));
            };
          }
        }

        @Override
        public Writer createRecordBuilder() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Void finishRecord(Writer unfinishedRecord) {
          throw new UnsupportedOperationException();
        }
      });

      // Write header
      boolean firstColumn = true;
      for (Channel channel : reader.getChannels()) {
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
      for (Channel channel : reader.getChannels()) {
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
        reader.nextInto(writer);
        writer.write(LINE_SEP);
      }
    }
  }
}


class CsvColumnDeserialize implements Deserialize<String> {

  @Override
  public String deserialize(Deserializer deserializer) throws IOException {
    return deserializer.deserialize_value(new Visitor<String, Void>() {
      @Override
      public String expecting() {
        return "CSV value";
      }

      @Override
      public String visitU8(byte value, Void param) {
        return UnsignedByte.toString(value);
      }

      @Override
      public String visitU16(short value, Void param) {
        return UnsignedShort.toString(value);
      }

      @Override
      public String visitU32(int value, Void param) {
        return UnsignedInteger.toString(value);
      }

      @Override
      public String visitU64(long value, Void param) {
        return UnsignedLong.toString(value);
      }

      @Override
      public String visitI8(byte value, Void param) {
        return Byte.toString(value);
      }

      @Override
      public String visitI16(short value, Void param) {
        return Short.toString(value);
      }

      @Override
      public String visitI32(int value, Void param) {
        return Integer.toString(value);
      }

      @Override
      public String visitI64(long value, Void param) {
        return Long.toString(value);
      }

      @Override
      public String visitF16(short value, Void param) {
        return Half.toString(value);
      }

      @Override
      public String visitF32(float value, Void param) {
        return Float.toString(value);
      }

      @Override
      public String visitF64(double value, Void param) {
        return Double.toString(value);
      }

      @Override
      public String visitString(String value, Void param) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
      }

      @Override
      public String visitByteArray(byte[] bytes, Void param) {
        return new String(Base64.getEncoder().encode(bytes), StandardCharsets.US_ASCII);
      }

      @Override
      public String visitInvalid(Void param) {
        return "";
      }
    }, null);
  }
}

