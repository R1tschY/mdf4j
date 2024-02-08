/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import static de.richardliebscher.mdf4.utils.Exceptions.wrapIOException;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.richardliebscher.mdf4.extract.DetachedRecordReader;
import de.richardliebscher.mdf4.extract.SerializableRecordFactory;
import de.richardliebscher.mdf4.extract.de.SerializableDeserializeInto;
import de.richardliebscher.mdf4.io.ByteBufferInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

public class ReferenceTest {

  private static Mdf4File openFileByByteBuffer() throws IOException {
    final ByteBuffer buffer;
    try (var resourceAsStream = ReferenceTest.class.getResourceAsStream(
        "/KonvektionKalt1-20140123-143636.mf4")) {
      assumeTrue(resourceAsStream != null);
      buffer = ByteBuffer.wrap(resourceAsStream.readAllBytes());
    }
    return Mdf4File.open(new ByteBufferInput(buffer));
  }

  private static Path getResourcePath(String name) {
    final var url = requireNonNull(
        ReferenceTest.class.getResource(name),
        "Resource " + name + " is missing");
    try {
      return Path.of(url.toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static void assertSameLines(String reference, List<String> actualLines) {
    List<String> expected = new ArrayList<>(actualLines.size());
    try (var lines = Files.lines(getResourcePath(reference), StandardCharsets.UTF_8)) {
      lines.filter(line -> !line.isEmpty()).forEachOrdered(expected::add);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    assertThat(actualLines.size()).isEqualTo(expected.size());

    int[] lineNo = new int[]{0};
    expected.forEach(refLine -> {
      lineNo[0] += 1;
      assertThat(actualLines.get(lineNo[0] - 1))
          .as("Line " + lineNo[0])
          .isEqualTo(refLine);
    });
  }

  @Test
  void testSimpleRecordReader() throws Exception {
    final var mdf4File = Mdf4File.open(getResourcePath("/KonvektionKalt1-20140123-143636.mf4"));

    var records = new ArrayList<String>();
    mdf4File.newRecordReader(new TestReprRecordFactory())
        .forEachRemaining(records::add);

    assertSameLines("/KonvektionKalt1-20140123-143636.csv", records);
  }

  @Test
  void testSimpleRecordReaderFromBytes() throws Exception {
    final var mdf4File = openFileByByteBuffer();

    var records = new ArrayList<String>();
    mdf4File.newRecordReader(new TestReprRecordFactory())
        .forEachRemaining(records::add);

    assertSameLines("/KonvektionKalt1-20140123-143636.csv", records);
  }

  @Test
  void checkIterDataGroups() throws IOException {
    final Mdf4File mdf4File = openFileByByteBuffer();

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
  void testParallelRecordReader() throws Exception {
    final var mdf4File = Mdf4File.open(getResourcePath("/KonvektionKalt1-20140123-143636.mf4"));

    final var threadIds = new HashSet<Long>();
    final var records = mdf4File.streamRecords(new TestReprRecordFactory())
        .parallel()
        .peek(ignored -> threadIds.add(Thread.currentThread().getId()))
        .map(Result::unwrap)
        .collect(Collectors.toList());

    assertThat(threadIds.size()).isGreaterThan(1);
    assertSameLines("/KonvektionKalt1-20140123-143636.csv", records);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testDistributedRecordReader() throws Exception {
    final Mdf4File mdf4File = openFileByByteBuffer();

    final var factory = new TestReprRecordFactory();

    final var records = mdf4File.splitRecordReaders(42, factory)
        .stream()
        .map(rr -> (DetachedRecordReader<?, String>) JavaSerde.de(JavaSerde.ser(rr)))
        .map(wrapIOException(mdf4File::attachRecordReader))
        .flatMap(recordReader -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(
            recordReader.iterator(), Spliterator.SIZED), false))
        .map(Result::unwrap)
        .collect(Collectors.toList());

    assertSameLines("/KonvektionKalt1-20140123-143636.csv", records);
  }

  private static class TestReprRecordFactory implements
      SerializableRecordFactory<StringBuilder, String> {

    private int index = 0;
    private final TestReprDeserialize de = new TestReprDeserialize();

    @Override
    public SerializableDeserializeInto<StringBuilder> selectChannel(DataGroup dg, ChannelGroup group, Channel channel) {
      SerializableDeserializeInto<StringBuilder> res = index == 0
          ? ((deserializer, dest) -> dest.append(de.deserialize(deserializer)))
          : ((deserializer, dest) -> {
            dest.append('|');
            dest.append(de.deserialize(deserializer));
          });
      index += 1;
      return res;
    }

    @Override
    public StringBuilder createRecordBuilder() {
      return new StringBuilder();
    }

    @Override
    public String finishRecord(StringBuilder unfinishedRecord) throws IOException {
      return unfinishedRecord.toString();
    }

    @Override
    public boolean selectGroup(DataGroup dg, ChannelGroup group) {
      return true;
    }
  }
}
