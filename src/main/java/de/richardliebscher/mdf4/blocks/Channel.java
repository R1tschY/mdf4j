/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.LazyIoIterator;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class Channel {

  Link<Channel> nextChannel;
  long component; // CA,CN
  Link<Text> channelName;
  Link<SourceInformation> channelSource; // SI
  Link<ChannelConversion> conversionRule;
  long signalData; // cnType=1:SD,DZ,DL,HL,CG,cnType=4:AT,cnType=5:CN,event:EV
  Link<TextBased> physicalUnit;
  Link<TextBased> comment;
  // 4.1.0
  // long atReference; // AT
  // long defaultXDg;long defaultXCg;long defaultXCn; // DG+CG,CN

  ChannelType type;
  SyncType syncType;
  ChannelDataType dataType;
  byte bitOffset;
  int byteOffset;
  int bitCount;
  ChannelFlags flags;
  int invalidationBit;
  byte precision;
  short attachmentCount;
  Range valueRange;
  Range limit;
  Range limitExtended;

  public static Channel parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(BlockType.CN, input, 8, 72);
    final var type = ChannelType.parse(input.readU8());
    final var syncType = SyncType.parse(input.readU8());
    final var dataType = ChannelDataType.parse(input.readU8());
    final var bitOffset = input.readU8();
    final var byteOffset = input.readI32();
    final var bitCount = input.readI32();
    final var flags = ChannelFlags.of(input.readI32());
    final var invalidationBit = input.readI32();
    final var precision = input.readU8();
    input.skip(1);
    final var attachmentCount = input.readI16();
    final var valueRange = new Range(input.readF64(), input.readF64());
    final var limit = new Range(input.readF64(), input.readF64());
    final var limitExtended = new Range(input.readF64(), input.readF64());

    final var links = blockHeader.getLinks();
    return new Channel(
        Link.of(links[0]), links[1], Link.of(links[2]), Link.of(links[3]), Link.of(links[4]),
        links[5],
        Link.of(links[6]), Link.of(links[7]),
        type, syncType, dataType, bitOffset, byteOffset, bitCount, flags,
        invalidationBit, precision, attachmentCount, valueRange, limit,
        limitExtended);
  }

  public static class Iterator implements LazyIoIterator<Channel> {

    private final ByteInput input;
    private Link<Channel> next;

    public Iterator(Link<Channel> start, ByteInput input) {
      this.input = input;
      this.next = start;
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public Channel next() throws IOException {
      final var channel = next.resolve(Channel.META, input)
          .orElseThrow();
      next = channel.getNextChannel();
      return channel;
    }
  }

  public static final Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Meta implements FromBytesInput<Channel> {

    @Override
    public Channel parse(ByteInput input) throws IOException {
      return Channel.parse(input);
    }
  }
}

