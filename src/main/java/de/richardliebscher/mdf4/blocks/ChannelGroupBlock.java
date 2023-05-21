/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class ChannelGroupBlock {

  Link<ChannelGroupBlock> nextChannelGroup;
  Link<Channel> firstChannel;
  Link<Text> acquisitionName;
  Link<SourceInformation> acquisitionSource;
  long firstSampleReduction; // SR
  Link<TextBased> comment;

  long recordId;
  long cycleCount;
  ChannelGroupFlags flags;
  char pathSeparator;
  int dataBytes;
  int invalidationBytes;

  public java.util.Iterator<Channel> iterChannels(ByteInput input) {
    return new Channel.Iterator(firstChannel, input);
  }

  public static ChannelGroupBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parse(BlockType.CG, input);
    final var links = blockHeader.getLinks();
    final Link<ChannelGroupBlock> nextChannelGroup = Link.of(links[0]);
    final Link<Channel> firstChannel = Link.of(links[1]);
    final Link<Text> acquisitionName = Link.of(links[2]);
    final Link<SourceInformation> acquisitionSource = Link.of(links[3]);
    final var firstSampleReduction = links[4];
    final Link<TextBased> comment = Link.of(links[5]);

    final var recordId = input.readI64();
    final var cycleCount = input.readI64();
    final var flags = ChannelGroupFlags.of(input.readI16());
    final var pathSeparator = input.readString(2, StandardCharsets.UTF_16LE).charAt(0);
    input.skip(4);
    final var dataBytes = input.readI32();
    final var invalidationBits = input.readI32();

    return new ChannelGroupBlock(
        nextChannelGroup, firstChannel, acquisitionName, acquisitionSource, firstSampleReduction,
        comment,
        recordId, cycleCount, flags, pathSeparator, dataBytes, invalidationBits);
  }

  public static class Iterator implements java.util.Iterator<ChannelGroupBlock> {

    private final ByteInput input;
    private Link<ChannelGroupBlock> next;

    public Iterator(Link<ChannelGroupBlock> start, ByteInput input) {
      this.input = input;
      this.next = start;
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public ChannelGroupBlock next() {
      try {
        final var channelGroup = next.resolve(ChannelGroupBlock.META, input)
            .orElseThrow();
        next = channelGroup.getNextChannelGroup();
        return channelGroup;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  public static final Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Meta implements FromBytesInput<ChannelGroupBlock> {

    @Override
    public ChannelGroupBlock parse(ByteInput input) throws IOException {
      return ChannelGroupBlock.parse(input);
    }
  }
}

