/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.LazyIoIterator;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class ChannelBlock implements Composition {

  Link<ChannelBlock> nextChannel;
  Link<Composition> composition;
  Link<TextBlock> channelName;
  Link<SourceInformationBlock> channelSource;
  Link<ChannelConversionBlock> conversionRule;
  Link<?> signalData; // cnType=1:SD,DZ,DL,HL,CG,cnType=4:AT,cnType=5:CN,event:EV
  Link<Metadata> physicalUnit;
  Link<Metadata> comment;
  // 4.1.0
  // long atReference; // AT
  // long defaultXDg;long defaultXCg;long defaultXCn; // DG+CG,CN

  ChannelType type;
  SyncType syncType;
  ChannelDataType dataType;
  byte bitOffset;
  int byteOffset;
  int bitCount;
  BitFlags<ChannelFlag> flags;
  int invalidationBit;
  byte precision;
  short attachmentCount;
  Range valueRange;
  Range limit;
  Range limitExtended;

  public static ChannelBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(ID, input, 8, 72);
    final var type = ChannelType.parse(input.readU8());
    final var syncType = SyncType.parse(input.readU8());
    final var dataType = ChannelDataType.parse(input.readU8());
    final var bitOffset = input.readU8();
    final var byteOffset = input.readI32();
    final var bitCount = input.readI32();
    final var flags = BitFlags.of(input.readI32(), ChannelFlag.class);
    final var invalidationBit = input.readI32();
    final var precision = input.readU8();
    input.skip(1);
    final var attachmentCount = input.readI16();
    final var valueRange = new Range(input.readF64(), input.readF64());
    final var limit = new Range(input.readF64(), input.readF64());
    final var limitExtended = new Range(input.readF64(), input.readF64());

    final var links = blockHeader.getLinks();
    return new ChannelBlock(
        Link.of(links[0]), Link.of(links[1]), Link.of(links[2]), Link.of(links[3]),
        Link.of(links[4]), Link.of(links[5]), Link.of(links[6]), Link.of(links[7]),
        type, syncType, dataType, bitOffset, byteOffset, bitCount, flags,
        invalidationBit, precision, attachmentCount, valueRange, limit,
        limitExtended);
  }

  public Optional<Integer> getPrecision() {
    if (flags.isSet(ChannelFlag.PRECISION_VALID)) {
      if (precision == (byte) 0xFF) {
        return Optional.of(Integer.MAX_VALUE);
      } else {
        return Optional.of(Byte.toUnsignedInt(precision));
      }
    }
    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  public Link<ChannelBlock> getMaxLengthChannel() {
    if (type != ChannelType.MAXIMUM_LENGTH_CHANNEL) {
      throw new IllegalStateException();
    }
    return (Link<ChannelBlock>) signalData;
  }

  public static class Iterator implements LazyIoIterator<ChannelBlock> {

    private final ByteInput input;
    private Link<ChannelBlock> next;

    public Iterator(Link<ChannelBlock> start, ByteInput input) {
      this.input = input;
      this.next = start;
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public ChannelBlock next() throws IOException {
      final var channel = next.resolve(ChannelBlock.TYPE, input)
          .orElseThrow();
      next = channel.getNextChannel();
      return channel;
    }
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('C', 'N');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements BlockType<ChannelBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public ChannelBlock parse(ByteInput input) throws IOException {
      return ChannelBlock.parse(input);
    }
  }
}

