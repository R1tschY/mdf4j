/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import static de.richardliebscher.mdf4.blocks.ChannelFlag.ALL_VALUES_INVALID;
import static de.richardliebscher.mdf4.blocks.ChannelFlag.INVALIDATION_BIT_VALID;

import de.richardliebscher.mdf4.blocks.BitFlags;
import de.richardliebscher.mdf4.blocks.ChannelBlock;
import de.richardliebscher.mdf4.blocks.ChannelConversionBlock;
import de.richardliebscher.mdf4.blocks.ChannelDataType;
import de.richardliebscher.mdf4.blocks.ChannelFlag;
import de.richardliebscher.mdf4.blocks.ChannelType;
import de.richardliebscher.mdf4.blocks.Composition;
import de.richardliebscher.mdf4.blocks.SyncType;
import de.richardliebscher.mdf4.blocks.TextBlock;
import de.richardliebscher.mdf4.datatypes.ByteArrayType;
import de.richardliebscher.mdf4.datatypes.DataType;
import de.richardliebscher.mdf4.datatypes.FloatType;
import de.richardliebscher.mdf4.datatypes.IntegerType;
import de.richardliebscher.mdf4.datatypes.StringType;
import de.richardliebscher.mdf4.datatypes.StructField;
import de.richardliebscher.mdf4.datatypes.StructType;
import de.richardliebscher.mdf4.datatypes.UnsignedIntegerType;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.internal.FileContext;
import java.io.IOException;
import java.util.ArrayList;
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
    return block.getChannelName().resolve(TextBlock.TYPE, ctx.getInput())
        .orElseThrow(() -> new FormatException("Channel name link is required"))
        .getText();
  }

  /**
   * Get information whether channel contains bus events.
   *
   * @return {@code true} iff "Bus event flag" is set.
   */
  public boolean containsBusEvent() {
    return block.getFlags().isSet(ChannelFlag.BUS_EVENT);
  }

  /**
   * Return Physical unit (after conversion).
   *
   * @return Physical unit (after conversion)
   * @throws IOException Failed to read from MDF file
   */
  public Optional<String> getPhysicalUnit() throws IOException {
    final var channelUnit = ctx.readText(block.getPhysicalUnit(),
        ChannelConversionBlock.UNIT_ELEMENT);
    if (channelUnit.isPresent()) {
      return channelUnit;
    }

    final var cc = block.getConversionRule().resolve(ChannelConversionBlock.TYPE, ctx.getInput());
    if (cc.isPresent()) {
      return ctx.readText(cc.get().getUnit(), ChannelConversionBlock.UNIT_ELEMENT);
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
   * Return RAW data type of channel values.
   *
   * @return Channel value data type
   */
  public DataType getDataType() throws IOException {
    return getDataTypeFromBlock(block, ctx);
  }

  private static DataType getDataTypeFromBlock(ChannelBlock block, FileContext ctx)
      throws IOException {
    final var compositionOptional = block.getComposition()
        .resolve(Composition.TYPE, ctx.getInput());
    if (compositionOptional.isPresent()) {
      final var composition = compositionOptional.get();
      if (composition instanceof ChannelBlock) {
        final var fields = new ArrayList<StructField>();
        fields.add(getStructField((ChannelBlock) composition, ctx));
        var iter = new ChannelBlock.Iterator(
            ((ChannelBlock) composition).getNextChannel(), ctx.getInput());
        while (iter.hasNext()) {
          fields.add(getStructField(iter.next(), ctx));
        }
        return new StructType(fields);
      } else {
        throw new NotImplementedFeatureException("Array composition not implemented");
      }
    }

    final ChannelDataType dataType;
    final int bitCount;
    final Integer precision;

    final var conversion = block.getConversionRule()
        .resolve(ChannelConversionBlock.TYPE, ctx.getInput());
    if (conversion.isPresent()) {
      final var conversionBlock = conversion.get();
      switch (conversionBlock.getType()) {
        case IDENTITY:
          precision = block.getPrecision().orElse(null);
          dataType = block.getDataType();
          bitCount = block.getType().equals(ChannelType.FIXED_LENGTH_DATA_CHANNEL)
              ? block.getBitCount() : -1;
          break;
        case LINEAR:
        case RATIONAL:
        case ALGEBRAIC:
        case INTERPOLATED_VALUE_TABLE:
        case VALUE_VALUE_TABLE:
        case VALUE_RANGE_VALUE_TABLE:
        case TEXT_VALUE_TABLE:
          dataType = ChannelDataType.FLOAT_LE;
          bitCount = 64;
          precision = conversionBlock.getPrecision().orElse(null);
          break;
        case TEXT_TEXT_TABLE:
        case BITFIELD_TEXT_TABLE:
          dataType = ChannelDataType.STRING_LATIN1;
          bitCount = -1;
          precision = null;
          break;
        default:
        case VALUE_TEXT_TABLE:
        case VALUE_RANGE_TEXT_TABLE:
          throw new NotImplementedFeatureException(
              "Not implemented conversion with maybe mixed data type");
      }

    } else {
      precision = block.getPrecision().orElse(null);
      dataType = block.getDataType();
      bitCount = block.getType() != ChannelType.VARIABLE_LENGTH_DATA_CHANNEL
          ? block.getBitCount() : -1;
    }

    switch (dataType) {
      case UINT_LE:
      case UINT_BE:
        return new UnsignedIntegerType(bitCount);
      case INT_LE:
      case INT_BE:
        return new IntegerType(bitCount);
      case FLOAT_LE:
      case FLOAT_BE:
        return new FloatType(bitCount, precision);
      case STRING_LATIN1:
      case STRING_UTF8:
      case STRING_UTF16LE:
      case STRING_UTF16BE:
        return new StringType(bitCount >= 0 ? bitCount / 8 : null);
      case BYTE_ARRAY:
        return new ByteArrayType(bitCount >= 0 ? bitCount / 8 : null);
      case MIME_SAMPLE:
      case MIME_STREAM:
      case CANOPEN_DATE:
      case CANOPEN_TIME:
      case COMPLEX_LE:
      case COMPLEX_BE:
      default:
        throw new IllegalStateException("Data type " + dataType + " not implemented");
    }
  }

  private static StructField getStructField(ChannelBlock block, FileContext ctx)
      throws IOException {
    final var dataType = getDataTypeFromBlock(block, ctx);
    final var channel = new Channel(block, ctx);
    return new StructField(channel.getName(), dataType);
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
          .resolve(ChannelBlock.TYPE, ctx.getInput())
          .orElse(null);
      if (dataGroup == null) {
        return null;
      }
      next = dataGroup.getNextChannel();
      return new Channel(dataGroup, ctx);
    }
  }
}
