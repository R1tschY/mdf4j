/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import static de.richardliebscher.mdf4.blocks.ChannelFlag.ALL_VALUES_INVALID;
import static de.richardliebscher.mdf4.blocks.ChannelFlag.INVALIDATION_BIT_VALID;

import de.richardliebscher.mdf4.blocks.BitFlags;
import de.richardliebscher.mdf4.blocks.ChannelBlock;
import de.richardliebscher.mdf4.blocks.ChannelConversion;
import de.richardliebscher.mdf4.blocks.ChannelFlag;
import de.richardliebscher.mdf4.blocks.ChannelType;
import de.richardliebscher.mdf4.blocks.SyncType;
import de.richardliebscher.mdf4.blocks.TextBlockBlock;
import de.richardliebscher.mdf4.datatypes.DataType;
import de.richardliebscher.mdf4.datatypes.FloatType;
import de.richardliebscher.mdf4.datatypes.IntegerType;
import de.richardliebscher.mdf4.datatypes.StringType;
import de.richardliebscher.mdf4.datatypes.UnsignedIntegerType;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.internal.FileContext;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * ChannelBlock.
 */
@RequiredArgsConstructor
public class Channel {

  private static final BitFlags<ChannelFlag> INVALID_FLAGS = BitFlags.of(
      ChannelFlag.class, INVALIDATION_BIT_VALID, ALL_VALUES_INVALID);

  private final ChannelBlock block;
  private final FileContext ctx;

  /**
   * Get low-level block structure.
   *
   * @return Corresponding MDF4 block
   */
  public ChannelBlock getBlock() {
    return block;
  }

  /**
   * Get channel name.
   *
   * @return ChannelBlock name
   * @throws IOException Failed to read from MDF file
   */
  public String getName() throws IOException {
    return block.getChannelName().resolve(TextBlockBlock.META, ctx.getInput())
        .orElseThrow(() -> new FormatException("Channel name link is required"))
        .getText();
  }

  /**
   * Return Physical unit (after conversion).
   *
   * @return Physical unit (after conversion)
   * @throws IOException Failed to read from MDF file
   */
  public Optional<String> getPhysicalUnit() throws IOException {
    final var channelUnit = ctx.readText(block.getPhysicalUnit(), ChannelConversion.UNIT_ELEMENT);
    if (channelUnit.isPresent()) {
      return channelUnit;
    }

    final var cc = block.getConversionRule().resolve(ChannelConversion.META, ctx.getInput());
    if (cc.isPresent()) {
      return ctx.readText(cc.get().getUnit(), ChannelConversion.UNIT_ELEMENT);
    }

    return Optional.empty();
  }

  /**
   * Return whether invalid values are possible.
   */
  public boolean isInvalidable() {
    return block.getFlags().anyOf(INVALID_FLAGS);
  }

  /**
   * Return data type of channel values.
   *
   * @return Channel value data type
   */
  public DataType getDataType() {
    // TODO: consider possible conversion
    switch (block.getDataType()) {
      case UINT_LE:
      case UINT_BE:
        return new UnsignedIntegerType(block.getBitCount());
      case INT_LE:
      case INT_BE:
        return new IntegerType(block.getBitCount());
      case FLOAT_LE:
      case FLOAT_BE:
        return new FloatType(
            block.getBitCount(), block.getPrecision().orElse(null));
      case STRING_LATIN1:
      case STRING_UTF8:
      case STRING_UTF16LE:
      case STRING_UTF16BE:
        return new StringType(block.getType().equals(ChannelType.FIXED_LENGTH_DATA_CHANNEL)
            ? block.getBitCount() / 8 : null);
      case BYTE_ARRAY:
      case MIME_SAMPLE:
      case MIME_STREAM:
      case CANOPEN_DATE:
      case CANOPEN_TIME:
      case COMPLEX_LE:
      case COMPLEX_BE:
      default:
        throw new IllegalStateException("Data type " + block.getDataType() + " not implemented");
    }
  }

  /**
   * Return whether this channel is a master channel.
   *
   * @return {@code true}, iff channel is a master channel
   */
  public boolean isMaster() {
    final var type = block.getType();
    return type == ChannelType.MASTER_CHANNEL || type == ChannelType.VIRTUAL_MASTER_CHANNEL;
  }

  /**
   * Return whether this channel is the time master channel.
   *
   * @return {@code true}, iff channel is the time master channel
   */
  public boolean isTimeMaster() {
    return block.getSyncType().equals(SyncType.TIME) && isMaster();
  }

  static class Iterator implements LazyIoIterator<Channel> {

    private final FileContext ctx;
    private Link<ChannelBlock> next;

    Iterator(Link<ChannelBlock> start, FileContext ctx) {
      this.ctx = ctx;
      this.next = start;
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public Channel next() throws IOException {
      final var dataGroup = next
          .resolve(ChannelBlock.META, ctx.getInput())
          .orElse(null);
      if (dataGroup == null) {
        return null;
      }
      next = dataGroup.getNextChannel();
      return new Channel(dataGroup, ctx);
    }
  }
}
