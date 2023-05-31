/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.examples;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.Mdf4File;
import de.richardliebscher.mdf4.extract.ChannelSelector;
import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.Half;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.extract.de.UnsignedByte;
import de.richardliebscher.mdf4.extract.de.UnsignedInteger;
import de.richardliebscher.mdf4.extract.de.UnsignedLong;
import de.richardliebscher.mdf4.extract.de.UnsignedShort;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.io.ByteBufferInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * CSV converter example build for simplicity.
 */
public class CsvExample {

  private static final String SEP = ",";
  private static final String LINE_SEP = "\n";

  public static void main(String[] args) throws Exception {
    final var source = Path.of(args[0]);
    final var target = Path.of(args[1]);

    // Open file
    final var input = new ByteBufferInput(ByteBuffer.wrap(Files.readAllBytes(source)));
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

    try (var writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
      final var recordDe = new RecordVisitor<Void>() {
        @Override
        public Void visitRecord(RecordAccess recordAccess) throws IOException {
          final var de = new CsvColumnDeserialize();

          while (recordAccess.remaining() != 0) {
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

