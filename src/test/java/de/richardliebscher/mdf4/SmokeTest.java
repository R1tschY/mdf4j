/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.richardliebscher.mdf4.extract.ChannelSelector;
import de.richardliebscher.mdf4.extract.de.ObjectDeserialize;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.extract.de.SerializableRecordVisitor;
import de.richardliebscher.mdf4.io.ByteBufferInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SmokeTest {

  @Test
  void test() throws Exception {
    final var mdf4File = Mdf4File.open(Path.of(requireNonNull(
        SmokeTest.class.getResource("/KonvektionKalt1-20140123-143636.mf4")).toURI()));

    final var channelSelector = new ChannelSelector() {
      @Override
      public boolean selectChannel(DataGroup dg, ChannelGroup group, Channel channel) {
        return true;
      }

      @Override
      public boolean selectGroup(DataGroup dg, ChannelGroup group) {
        return true;
      }
    };

    final var rowDe = new RecordVisitor<List<Object>>() {
      @Override
      public List<Object> visitRecord(RecordAccess rowAccess) throws IOException {
        final var de = new ObjectDeserialize();

        List<Object> objects = new ArrayList<>();
        while (rowAccess.remaining() != 0) {
          objects.add(rowAccess.nextElement(de));
        }
        return objects;
      }
    };

    final var reader = mdf4File.newRecordReader(channelSelector, rowDe);
    while (reader.remaining() != 0) {
      System.out.println(reader.next());
    }
  }

  @Test
  void checkIterDataGroups() throws IOException {
    final ByteBuffer buffer;
    try (var resourceAsStream = SmokeTest.class.getResourceAsStream(
            "/KonvektionKalt1-20140123-143636.mf4")) {
      assumeTrue(resourceAsStream != null);
      buffer = ByteBuffer.wrap(resourceAsStream.readAllBytes());
    }
    final var input = new ByteBufferInput(buffer);
    final var mdf4File = Mdf4File.open(input);

    final var iterator = mdf4File.getDataGroups().iter();
    DataGroup dataGroup;
    while ((dataGroup = iterator.next()) != null) {
      System.out.printf("DG: %s%n", dataGroup.getName());

      final var cgIterator = dataGroup.getChannelGroups().iter();
      ChannelGroup channelGroup;
      while ((channelGroup = cgIterator.next()) != null) {
        System.out.printf("  CG: %s%n", channelGroup.getName());

        final var cnIterator = channelGroup.getChannels().iter();
        Channel channel;
        while ((channel = cnIterator.next()) != null) {
          System.out.printf("    CN: %s%n", channel.getName());
        }
      }
    }
  }

  @Test
  void testParallel() throws Exception {
    final ByteBuffer buffer;
    try (var resourceAsStream = SmokeTest.class.getResourceAsStream(
            "/KonvektionKalt1-20140123-143636.mf4")) {
      assumeTrue(resourceAsStream != null);
      buffer = ByteBuffer.wrap(resourceAsStream.readAllBytes());
    }
    final var input = new ByteBufferInput(buffer);
    final var mdf4File = Mdf4File.open(input);

    final var channelSelector = new ChannelSelector() {
      @Override
      public boolean selectChannel(DataGroup dg, ChannelGroup group, Channel channel) {
        return true;
      }

      @Override
      public boolean selectGroup(DataGroup dg, ChannelGroup group) {
        return true;
      }
    };

    final var rowDe = new SerializableRecordVisitor<List<Object>>() {
      @Override
      public List<Object> visitRecord(RecordAccess rowAccess) throws IOException {
        final var de = new ObjectDeserialize();

        List<Object> objects = new ArrayList<>();
        while (rowAccess.remaining() != 0) {
          objects.add(rowAccess.nextElement(de));
        }
        return objects;
      }
    };

    mdf4File.streamRecords(channelSelector, rowDe)
            .parallel()
            .forEachOrdered(System.out::println);
  }

}
