/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import static de.richardliebscher.mdf4.internal.Iterators.asIterable;
import static de.richardliebscher.mdf4.internal.Iterators.streamBlockSeq;

import de.richardliebscher.mdf4.blocks.Channel;
import de.richardliebscher.mdf4.blocks.ChannelConversion;
import de.richardliebscher.mdf4.blocks.ChannelFlags;
import de.richardliebscher.mdf4.blocks.ChannelGroup;
import de.richardliebscher.mdf4.blocks.ChannelGroupFlags;
import de.richardliebscher.mdf4.blocks.DataGroupBlock;
import de.richardliebscher.mdf4.blocks.Text;
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
import de.richardliebscher.mdf4.internal.InternalReader;
import de.richardliebscher.mdf4.internal.Pair;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
  private final ChannelGroup group;
  private final RecordBuffer input;
  private long cycle = 0;

  private RecordReader(
      List<ValueRead> channelReaders, RecordVisitor<R> factory, DataRead dataSource,
      ChannelGroup group) {
    this.channelReaders = channelReaders;
    this.factory = factory;
    this.dataSource = dataSource;
    this.buffer = ByteBuffer.allocate(group.getDataBytes() + group.getInvalidationBytes());
    this.input = new RecordByteBuffer(buffer);
    this.group = group;
  }

  // PUBLIC

  /**
   * Read next record.
   *
   * @return Deserialized record or {@code null} if no record is left
   * @throws IOException Unable to read record from file
   */
  public R next() throws IOException {
    if (cycle >= group.getCycleCount()) {
      return null;
    }

    cycle += 1;
    buffer.clear();
    final var bytes = dataSource.read(buffer);
    if (bytes != buffer.capacity()) {
      throw new FormatException(
          "Early end of data at cycle " + cycle + " of " + group.getCycleCount());
    }

    return factory.visitRecord(new RecordAccess() {
      private final Iterator<ValueRead> iterator = channelReaders.iterator();

      @Override
      public <S extends DeserializeSeed<T>, T> T nextElementSeed(Deserialize<T> deserialize, S seed)
          throws IOException {
        if (iterator.hasNext()) {
          final var valueRead = iterator.next();
          return seed.deserialize(deserialize, new Deserializer() {

            @Override
            public <R2> R2 deserialize_value(Visitor<R2> visitor) throws IOException {
              return valueRead.read(input, visitor);
            }
          });
        } else {
          return null;
        }
      }

      @Override
      public int size() {
        return channelReaders.size();
      }
    });
  }

  // STATIC

  private static ValueRead createChannelReader(
      DataGroupBlock dataGroup, ChannelGroup group,
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
      DataGroupBlock dataGroup, ChannelGroup group, Channel channel, ValueRead valueRead)
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
  static <R> RecordReader<R> createFor(InternalReader reader, ChannelSelector selector,
      RecordVisitor<R> rowDeserializer)
      throws ChannelGroupNotFoundException, IOException {
    final var input = reader.getInput();

    // select
    final var group = streamBlockSeq(reader.getHeader().iterDataGroups(input))
        .flatMap(dg -> streamBlockSeq(dg.iterChannelGroups(input)).map(cg -> Pair.of(dg, cg)))
        .filter(g -> selector.selectGroup(g.getLeft(), g.getRight()))
        .findFirst()
        .orElseThrow(() -> new ChannelGroupNotFoundException("No matching channel group found"));

    final var dataGroup = group.getLeft();
    if (dataGroup.getRecordIdSize() != 0) {
      throw new NotImplementedFeatureException("Unsorted data groups not implemented");
    }

    final var channelGroup = group.getRight();
    if (!channelGroup.getFlags().equals(ChannelGroupFlags.of(0))) {
      throw new NotImplementedFeatureException("Any channel group flags not implemented");
    }

    // data source
    final var source = DataSource.create(reader, dataGroup);

    // build extractor
    //noinspection ConstantValue
    log.finest(() ->
        "Record size: " + (dataGroup.getRecordIdSize() + channelGroup.getDataBytes()
            + channelGroup.getInvalidationBytes())
            + " (RecordId: " + dataGroup.getRecordIdSize() + ", Data: "
            + channelGroup.getDataBytes()
            + ", InvalidationBytes: " + channelGroup.getInvalidationBytes() + ")");

    final var channelReaders = new ArrayList<ValueRead>();
    for (var ch : asIterable(() -> channelGroup.iterChannels(input))) {
      try {
        final var channelReader = createChannelReader(dataGroup, channelGroup, ch, input);
        if (selector.selectChannel(dataGroup, channelGroup, ch)) {
          log.finest(() -> "Channel read offset: " + ch.getByteOffset());
          channelReaders.add(channelReader);
        }
      } catch (NotImplementedFeatureException exception) {
        log.warning("Ignoring channel '" + ch.getChannelName().resolve(Text.META, input) + "': "
            + exception.getMessage());
      }
    }

    return new RecordReader<>(channelReaders, rowDeserializer, source, channelGroup);
  }
}
