/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.richardliebscher.mdf4.blocks.Channel;
import de.richardliebscher.mdf4.blocks.ChannelGroup;
import de.richardliebscher.mdf4.blocks.DataGroupBlock;
import de.richardliebscher.mdf4.extract.ChannelSelector;
import de.richardliebscher.mdf4.extract.de.ObjectDeserialize;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.io.ByteBufferInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SmokeTest {

  @Test
  void test() throws Exception {
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
      public boolean selectChannel(DataGroupBlock dg, ChannelGroup group, Channel channel) {
        return true;
      }

      @Override
      public boolean selectGroup(DataGroupBlock dg, ChannelGroup group) {
        return true;
      }
    };

    final var rowDe = new RecordVisitor<List<Object>>() {
      @Override
      public List<Object> visitRecord(RecordAccess rowAccess) throws IOException {
        final var de = new ObjectDeserialize();

        List<Object> objects = new ArrayList<>();
        Object elem;
        while ((elem = rowAccess.nextElement(de)) != null) {
          objects.add(elem);
        }
        return objects;
      }
    };

    final var newRowReader = mdf4File.newRecordReader(channelSelector, rowDe);

    List<Object> row;
    while ((row = newRowReader.next()) != null) {
      System.out.println(row);
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

    final var iterator = mdf4File.iterDataGroups();
    while (iterator.hasNext()) {
      final var dataGroup = iterator.next();
      System.out.printf("DG: %s%n", dataGroup.getName());

      final var cgIterator = dataGroup.iterChannelGroups();
      while (cgIterator.hasNext()) {
        final var channelGroup = cgIterator.next();
        System.out.printf("  CG: %s%n", channelGroup.getName());

        final var cnIterator = channelGroup.iterChannels();
        while (cnIterator.hasNext()) {
          final var channel = cnIterator.next();
          System.out.printf("    CN: %s%n", channel.getName());
        }
      }
    }
  }

}
