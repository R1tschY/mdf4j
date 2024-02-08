/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.LazyIoIterator;
import de.richardliebscher.mdf4.LazyIoList;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class ChannelGroupBlock {

  Link<ChannelGroupBlock> nextChannelGroup;
  Link<ChannelBlock> firstChannel;
  Link<TextBlock> acquisitionName;
  Link<SourceInformationBlock> acquisitionSource;
  long firstSampleReduction; // SR
  Link<Metadata> comment;

  long recordId;
  long cycleCount;
  BitFlags<ChannelGroupFlag> flags;
  char pathSeparator;
  int dataBytes;
  int invalidationBytes;

  public LazyIoList<ChannelBlock> getChannels(ByteInput input) {
    return () -> new ChannelBlock.Iterator(firstChannel, input);
  }

  public static ChannelGroupBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parse(ID, input);
    final var links = blockHeader.getLinks();
    final Link<ChannelGroupBlock> nextChannelGroup = Link.of(links[0]);
    final Link<ChannelBlock> firstChannel = Link.of(links[1]);
    final Link<TextBlock> acquisitionName = Link.of(links[2]);
    final Link<SourceInformationBlock> acquisitionSource = Link.of(links[3]);
    final var firstSampleReduction = links[4];
    final Link<Metadata> comment = Link.of(links[5]);

    final var recordId = input.readI64();
    final var cycleCount = input.readI64();
    final var flags = BitFlags.of(input.readI16(), ChannelGroupFlag.class);
    final var pathSeparator = input.readString(2, StandardCharsets.UTF_16LE).charAt(0);
    input.skip(4);
    final var dataBytes = input.readI32();
    final var invalidationBits = input.readI32();

    return new ChannelGroupBlock(
        nextChannelGroup, firstChannel, acquisitionName, acquisitionSource, firstSampleReduction,
        comment,
        recordId, cycleCount, flags, pathSeparator, dataBytes, invalidationBits);
  }

  public static class Iterator implements LazyIoIterator<ChannelGroupBlock> {

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
    public ChannelGroupBlock next() throws IOException {
      final var channelGroup = next.resolve(TYPE, input)
          .orElseThrow();
      next = channelGroup.getNextChannelGroup();
      return channelGroup;
    }
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('C', 'G');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements BlockType<ChannelGroupBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public ChannelGroupBlock parse(ByteInput input) throws IOException {
      return ChannelGroupBlock.parse(input);
    }
  }
}

