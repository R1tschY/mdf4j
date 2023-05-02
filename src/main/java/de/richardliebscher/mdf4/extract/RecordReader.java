/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import static de.richardliebscher.mdf4.internal.Iterators.asIterable;

import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.blocks.Channel;
import de.richardliebscher.mdf4.blocks.ChannelConversion;
import de.richardliebscher.mdf4.blocks.ChannelFlags;
import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.blocks.ChannelGroupFlags;
import de.richardliebscher.mdf4.blocks.DataGroupBlock;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.DeserializeSeed;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.InvalidDeserializer;
import de.richardliebscher.mdf4.extract.de.RecordAccess;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.extract.read.DataRead;
import de.richardliebscher.mdf4.extract.read.DataSource;
import de.richardliebscher.mdf4.extract.read.LinearConversion;
import de.richardliebscher.mdf4.extract.read.NoValueRead;
import de.richardliebscher.mdf4.extract.read.RecordBuffer;
import de.richardliebscher.mdf4.extract.read.RecordByteBuffer;
import de.richardliebscher.mdf4.extract.read.ValueRead;
import de.richardliebscher.mdf4.internal.FileContext;
import de.richardliebscher.mdf4.internal.Pair;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.java.Log;

/**
 * Read records into user-defined type.
 *
 * @param <R> User-defined record type
 * @see de.richardliebscher.mdf4.Mdf4File#newRecordReader
 */
@Log
public class RecordReader<R> {

  private final List<ValueRead> channelReaders;
  private final RecordVisitor<R> factory;
  private final DataRead dataSource;
  private final ByteBuffer buffer;
  private final ChannelGroupBlock group;
  private final RecordBuffer input;
  private long cycle = 0;

  private RecordReader(
      List<ValueRead> channelReaders, RecordVisitor<R> factory, DataRead dataSource,
      ChannelGroupBlock group) {
    this.channelReaders = channelReaders;
    this.factory = factory;
    this.dataSource = dataSource;
    this.buffer = ByteBuffer.allocate(group.getDataBytes() + group.getInvalidationBytes());
    this.input = new RecordByteBuffer(buffer);
    this.group = group;
  }

  // PUBLIC

  /**
   * Get number of records.
   *
   * @return number of records
   */
  public long size() {
    return group.getCycleCount();
  }

  /**
   * Get number of remaining records.
   *
   * @return number of remaining records
   */
  public long remaining() {
    return group.getCycleCount() - cycle;
  }

  /**
   * Create an iterator for deserializing all elements the same way.
   *
   * @return Deserialized value
   */
  public Iterator<R> iterator() {
    return new Iterator<>() {
      private long index = 0;
      private final long size = size();

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public R next() {
        try {
          index += 1;
          return RecordReader.this.next();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    };
  }

  /**
   * Read next record.
   *
   * @return Deserialized record
   * @throws IOException            Unable to read record from file
   * @throws NoSuchElementException No remaining records
   * @see #remaining()
   */
  public R next() throws IOException, NoSuchElementException {
    if (cycle >= group.getCycleCount()) {
      throw new NoSuchElementException();
    }

    cycle += 1;
    buffer.clear();
    final var bytes = dataSource.read(buffer);
    if (bytes != buffer.capacity()) {
      throw new FormatException(
          "Early end of data at cycle " + cycle + " of " + group.getCycleCount());
    }

    return factory.visitRecord(new RecordAccess() {
      private int index = 0;
      private final int size = channelReaders.size();

      @Override
      public <S extends DeserializeSeed<T>, T> T nextElementSeed(Deserialize<T> deserialize, S seed)
          throws IOException {
        if (index >= size) {
          throw new NoSuchElementException();
        }

        final var ret = seed.deserialize(deserialize, new Deserializer() {
          @Override
          public <R2> R2 deserialize_value(Visitor<R2> visitor) throws IOException {
            return channelReaders.get(index).read(input, visitor);
          }
        });
        index += 1;
        return ret;
      }

      @Override
      public int remaining() {
        return channelReaders.size() - index;
      }
    });
  }

  // STATIC

  private static ValueRead createChannelReader(
      DataGroupBlock dataGroup, ChannelGroupBlock group,
      Channel channel, ByteInput input) throws IOException {
    if (channel.getBitOffset() != 0) {
      throw new NotImplementedFeatureException("Non-zero bit offset is not implemented");
    }
    if (channel.getFlags().test(ChannelFlags.ALL_VALUES_INVALID)) {
      return new NoValueRead(new InvalidDeserializer());
    }

    ValueRead valueRead;
    switch (channel.getType()) {
      case FIXED_LENGTH_DATA_CHANNEL:
      case MASTER_CHANNEL:
        valueRead = createFixedLengthDataReader(channel);
        break;
      case VIRTUAL_MASTER_CHANNEL:
      case VIRTUAL_DATA_CHANNEL:
        valueRead = createVirtualMasterReader(channel);
        break;
      case VARIABLE_LENGTH_DATA_CHANNEL:
      case SYNCHRONIZATION_CHANNEL:
      case MAXIMUM_LENGTH_CHANNEL:
      default:
        throw new NotImplementedFeatureException(
            "Channel type not implemented: " + channel.getType());
    }

    final var channelConversion = channel.getConversionRule()
        .resolve(ChannelConversion.META, input);
    if (channelConversion.isPresent()) {
      final var cc = channelConversion.get();
      switch (cc.getType()) {
        case IDENTITY:
          break;
        case LINEAR:
          valueRead = new LinearConversion(cc, valueRead);
          break;
        case RATIONAL:
        case ALGEBRAIC:
        case INTERPOLATED_VALUE_TABLE:
        case VALUE_VALUE_TABLE:
        case VALUE_RANGE_VALUE_TABLE:
        case VALUE_TEXT_TABLE:
        case VALUE_RANGE_TEXT_TABLE:
        case TEXT_VALUE_TABLE:
        case TEXT_TEXT_TABLE:
        case BITFIELD_TEXT_TABLE:
        default:
          throw new NotImplementedFeatureException(
              "Channel conversion not implemented: " + cc.getType());
      }
    }

    if (channel.getFlags().test(ChannelFlags.INVALIDATION_BIT_VALID)) {
      valueRead = createInvalidationReader(dataGroup, group, channel, valueRead);
    }

    return valueRead;
  }

  private static ValueRead createInvalidationReader(
      DataGroupBlock dataGroup, ChannelGroupBlock group, Channel channel, ValueRead valueRead)
      throws FormatException {
    final var groupBits = group.getInvalidationBytes() * 8;
    final var invalidationBit = channel.getInvalidationBit();
    if (invalidationBit >= groupBits) {
      throw new FormatException("Invalid invalidation bit position "
          + invalidationBit + " in " + groupBits + " invalidation bits");
    }

    // PERF: Read invalidation byte(s) only once per record
    final var invalidationByteIndex =
        dataGroup.getRecordIdSize() + group.getDataBytes() + (invalidationBit >>> 3);
    final var invalidationBitMask = 1 << (invalidationBit & 0x07);
    return new ValueRead() {
      @Override
      public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
        if ((input.readU8(invalidationByteIndex) & invalidationBitMask) != 0) {
          return visitor.visitInvalid();
        } else {
          return valueRead.read(input, visitor);
        }
      }
    };
  }

  private static ValueRead createFixedLengthDataReader(Channel channel)
      throws NotImplementedFeatureException {
    switch (channel.getDataType()) {
      case UINT_LE:
        return createUintLeRead(channel);
      case UINT_BE:
        return createUintBeRead(channel);
      case INT_LE:
        return createIntLeRead(channel);
      case INT_BE:
        return createIntBeRead(channel);
      case FLOAT_LE:
        return createFloatLeRead(channel);
      case FLOAT_BE:
        return createFloatBeRead(channel);
      case STRING_LATIN1:
      case STRING_UTF8:
      case STRING_UTF16LE:
      case STRING_UTF16BE:
      case BYTE_ARRAY:
      case MIME_SAMPLE:
      case MIME_STREAM:
      case CANOPEN_DATE:
      case CANOPEN_TIME:
      case COMPLEX_LE:
      case COMPLEX_BE:
      default:
        throw new NotImplementedFeatureException(
            "Reading data type " + channel.getDataType() + " not implemented");
    }
  }

  private static ValueRead createUintLeRead(Channel channel) throws NotImplementedFeatureException {
    final var byteOffset = channel.getByteOffset();
    switch (channel.getBitCount()) {
      case 8:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitU8(input.readU8(byteOffset));
          }
        };
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitU16(input.readI16Le(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitU32(input.readI32Le(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitU64(input.readI64Le(byteOffset));
          }
        };
      default:
        throw new NotImplementedFeatureException(
            "Only integer with 1, 2, 4 or 8 bytes are implemented");
    }
  }

  private static ValueRead createUintBeRead(Channel channel) throws NotImplementedFeatureException {
    final var byteOffset = channel.getByteOffset();
    switch (channel.getBitCount()) {
      case 8:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitU8(input.readU8(byteOffset));
          }
        };
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitU16(input.readI16Be(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitU32(input.readI32Be(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitU64(input.readI64Be(byteOffset));
          }
        };
      default:
        throw new NotImplementedFeatureException(
            "Only integer with 1, 2, 4 or 8 bytes are implemented");
    }
  }

  private static ValueRead createIntLeRead(Channel channel) throws NotImplementedFeatureException {
    final var byteOffset = channel.getByteOffset();
    switch (channel.getBitCount()) {
      case 8:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitI8(input.readU8(byteOffset));
          }
        };
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitI16(input.readI16Le(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitI32(input.readI32Le(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitI64(input.readI64Le(byteOffset));
          }
        };
      default:
        throw new NotImplementedFeatureException(
            "Only integer with 1, 2, 4 or 8 bytes are implemented");
    }
  }

  private static ValueRead createIntBeRead(Channel channel) throws NotImplementedFeatureException {
    final var byteOffset = channel.getByteOffset();
    switch (channel.getBitCount()) {
      case 8:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitI8(input.readU8(byteOffset));
          }
        };
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitI16(input.readI16Be(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitI32(input.readI32Be(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitI64(input.readI64Be(byteOffset));
          }
        };
      default:
        throw new NotImplementedFeatureException(
            "Only integer with 1, 2, 4 or 8 bytes are implemented");
    }
  }

  private static ValueRead createFloatLeRead(Channel channel)
      throws NotImplementedFeatureException {
    final var byteOffset = channel.getByteOffset();
    switch (channel.getBitCount()) {
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitF32(input.readF32Le(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitF64(input.readF64Le(byteOffset));
          }
        };
      default:
        throw new NotImplementedFeatureException(
            "Only floating point numbers with 32 or 64 bits are implemented, got "
                + channel.getBitCount() + " bits");
    }
  }

  private static ValueRead createFloatBeRead(Channel channel)
      throws NotImplementedFeatureException {
    final var byteOffset = channel.getByteOffset();
    switch (channel.getBitCount()) {
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitF32(input.readF32Be(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitF64(input.readF64Be(byteOffset));
          }
        };
      default:
        throw new NotImplementedFeatureException(
            "Only floating point numbers with 4 or 8 bytes are implemented");
    }
  }

  private static ValueRead createVirtualMasterReader(Channel channel) throws FormatException {
    final ValueRead valueRead;
    if (channel.getBitCount() != 0) {
      throw new FormatException("Bit count of virtual master channel must be zero, but got "
          + channel.getBitCount());
    }
    // TODO
    // if (channel.getDataType() != ChannelDataType.UINT_LE) {
    //     throw new FormatException("Data type of virtual master channel must be little endian
    //     unsigned integer, but got "
    //             + channel.getDataType());
    // }
    // TODO: apply offset from HD block
    valueRead = new ValueRead() {
      private int recordIndex = 0;

      @Override
      public <T> T read(RecordBuffer input, Visitor<T> visitor) {
        return visitor.visitU32(recordIndex++);
      }
    };
    return valueRead;
  }

  /**
   * Internal API: Use {@link de.richardliebscher.mdf4.Mdf4File#newRecordReader}.
   *
   * @see de.richardliebscher.mdf4.Mdf4File#newRecordReader
   */
  static <R> RecordReader<R> createFor(FileContext ctx, Iterator<DataGroup> dataGroups,
      ChannelSelector selector,
      RecordVisitor<R> rowDeserializer)
      throws ChannelGroupNotFoundException, IOException {
    final var input = ctx.getInput();

    // select
    final var group = select(dataGroups, selector);

    final var dataGroup = group.getLeft();
    final var dataGroupBlock = dataGroup.getBlock();
    if (dataGroupBlock.getRecordIdSize() != 0) {
      throw new NotImplementedFeatureException("Unsorted data groups not implemented");
    }

    final var channelGroup = group.getRight();
    final var channelGroupBlock = channelGroup.getBlock();
    if (!channelGroupBlock.getFlags().equals(ChannelGroupFlags.of(0))) {
      throw new NotImplementedFeatureException("Any channel group flags not implemented");
    }

    // data source
    final var source = DataSource.create(ctx, dataGroupBlock);

    // build extractor
    //noinspection ConstantValue
    log.finest(() ->
        "Record size: "
            + (dataGroupBlock.getRecordIdSize() + channelGroupBlock.getDataBytes()
            + channelGroupBlock.getInvalidationBytes())
            + " (RecordId: " + dataGroupBlock.getRecordIdSize() + ", Data: "
            + channelGroupBlock.getDataBytes()
            + ", InvalidationBytes: " + channelGroupBlock.getInvalidationBytes() + ")");

    final var channelReaders = new ArrayList<ValueRead>();
    for (var ch : asIterable(channelGroup::iterChannels)) {
      try {
        final var channelReader = createChannelReader(
            dataGroupBlock, channelGroupBlock, ch.getBlock(), input);
        if (selector.selectChannel(dataGroup, channelGroup, ch)) {
          log.finest(() -> "Channel read offset: " + ch.getBlock().getByteOffset());
          channelReaders.add(channelReader);
        }
      } catch (NotImplementedFeatureException exception) {
        log.warning("Ignoring channel '" + ch.getName() + "': " + exception.getMessage());
      }
    }

    return new RecordReader<>(channelReaders, rowDeserializer, source, channelGroupBlock);
  }

  private static Pair<DataGroup, ChannelGroup> select(
      Iterator<DataGroup> dataGroups, ChannelSelector selector)
      throws ChannelGroupNotFoundException {
    while (dataGroups.hasNext()) {
      final var dataGroup = dataGroups.next();

      final var channelGroups = dataGroup.iterChannelGroups();
      while (channelGroups.hasNext()) {
        final var channelGroup = channelGroups.next();

        if (selector.selectGroup(dataGroup, channelGroup)) {
          return Pair.of(dataGroup, channelGroup);
        }
      }
    }

    throw new ChannelGroupNotFoundException("No matching channel group found");
  }
}
