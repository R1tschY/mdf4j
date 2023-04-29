/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.examples;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.richardliebscher.mdf4.Mdf4File;
import de.richardliebscher.mdf4.blocks.Channel;
import de.richardliebscher.mdf4.blocks.ChannelGroup;
import de.richardliebscher.mdf4.blocks.DataGroup;
import de.richardliebscher.mdf4.blocks.Text;
import de.richardliebscher.mdf4.blocks.TextBased;
import de.richardliebscher.mdf4.extract.ChannelSelector;
import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
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
    final var path = Path.of("/home/richard/Downloads/perftest.csv");

    // Open file
    final ByteBuffer buffer;
    try (var resourceAsStream = CsvExample.class.getResourceAsStream("/perftest.z.mf4")) {
      assumeTrue(resourceAsStream != null);
      buffer = ByteBuffer.wrap(resourceAsStream.readAllBytes());
    }
    final var input = new ByteBufferInput(buffer);
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

    try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      // Write header
      boolean firstColumn = true;
      for (Channel channel : channels) {
        if (firstColumn) {
          firstColumn = false;
        } else {
          writer.write(SEP);
        }

        writer.write(channel.getChannelName().resolve(Text.META, input)
            .map(Text::getData).orElse(""));
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

        writer.write(channel.getPhysicalUnit().resolve(TextBased.META, input)
            .map(text -> ((Text) text).getData()).orElse(""));
      }
      writer.write(LINE_SEP);

      // Write values
      final var nothing = new Object();
      final var recordDe = new RecordVisitor<>() {
        @Override
        public Object visitRecord(RecordAccess recordAccess) throws IOException {
          final var de = new CsvColumnDeserialize();

          boolean firstColumn = true;
          String elem;
          while ((elem = recordAccess.nextElement(de)) != null) {
            if (firstColumn) {
              firstColumn = false;
            } else {
              writer.write(SEP);
            }
            writer.write(elem);
          }

          writer.write(LINE_SEP);
          return nothing;
        }
      };

      final var newRecordReader = mdf4File.newRecordReader(channelSelector, recordDe);

      //noinspection StatementWithEmptyBody
      while (newRecordReader.next() != null) {
        // noop
      }
    }
  }

}


class CsvColumnDeserialize implements Deserialize<String> {

  @Override
  public String deserialize(Deserializer deserializer) throws IOException {
    return deserializer.deserialize_value(new Visitor<>() {
      @Override
      public String visitU8(byte value) {
        return String.valueOf(Byte.toUnsignedInt(value));
      }

      @Override
      public String visitU16(short value) {
        return String.valueOf(Short.toUnsignedInt(value));
      }

      @Override
      public String visitU32(int value) {
        return String.valueOf(Integer.toUnsignedLong(value));
      }

      @Override
      public String visitU64(long value) {
        return Long.toUnsignedString(value);
      }

      @Override
      public String visitI8(byte value) {
        return String.valueOf(value);
      }

      @Override
      public String visitI16(short value) {
        return String.valueOf(value);
      }

      @Override
      public String visitI32(int value) {
        return String.valueOf(value);
      }

      @Override
      public String visitI64(long value) {
        return String.valueOf(value);
      }

      @Override
      public String visitF32(float value) {
        return String.valueOf(value);
      }

      @Override
      public String visitF64(double value) {
        return String.valueOf(value);
      }

      @Override
      public String visitInvalid() {
        return "";
      }
    });
  }
}

