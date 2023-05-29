/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.LazyIoList;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.Channel;
import de.richardliebscher.mdf4.blocks.ChannelConversion;
import de.richardliebscher.mdf4.blocks.ChannelFlags;
import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.blocks.ChannelGroupFlags;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataGroupBlock;
import de.richardliebscher.mdf4.blocks.DataList;
import de.richardliebscher.mdf4.blocks.DataRoot;
import de.richardliebscher.mdf4.blocks.HeaderList;
import de.richardliebscher.mdf4.blocks.Text;
import de.richardliebscher.mdf4.blocks.ZipType;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.extract.de.InvalidDeserializer;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.extract.de.SerializableRecordVisitor;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.extract.read.ByteBufferRead;
import de.richardliebscher.mdf4.extract.read.DataListRead;
import de.richardliebscher.mdf4.extract.read.DataRead;
import de.richardliebscher.mdf4.extract.read.EmptyDataRead;
import de.richardliebscher.mdf4.extract.read.LinearConversion;
import de.richardliebscher.mdf4.extract.read.NoValueRead;
import de.richardliebscher.mdf4.extract.read.RecordBuffer;
import de.richardliebscher.mdf4.extract.read.ValueRead;
import de.richardliebscher.mdf4.internal.FileContext;
import de.richardliebscher.mdf4.internal.Pair;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RecordReaderFactory {

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
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitF16(input.readI16Le(byteOffset));
          }
        };
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
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) {
            return visitor.visitF16(input.readI16Be(byteOffset));
          }
        };
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
  static <R> RecordReader<R> createFor(FileContext ctx, LazyIoList<DataGroup> dataGroups,
                                       ChannelSelector selector, RecordVisitor<R> rowDeserializer)
          throws ChannelGroupNotFoundException, IOException {
    final var input = ctx.getInput();

    // select
    final var group = selectChannels(dataGroups, selector);
    final var dataGroup = group.getLeft();
    final var channelGroup = group.getRight();

    // data source
    final var source = createSource(ctx, dataGroup.getBlock());

    // build extractor
    final var channelReaders = buildExtractors(selector, input, dataGroup, channelGroup);

    return new RecordReader<>(channelReaders, rowDeserializer, source, channelGroup.getBlock());
  }

  static <R> ParallelRecordReader<R> createParallelFor(
          FileContext ctx, LazyIoList<DataGroup> dataGroups, ChannelSelector selector,
          SerializableRecordVisitor<R> rowDeserializer)
          throws ChannelGroupNotFoundException, IOException {
    final var input = ctx.getInput();

    // select
    final var group = selectChannels(dataGroups, selector);
    final var dataGroup = group.getLeft();
    final var channelGroup = group.getRight();

    // data source
    final var dataList = collectDataList(ctx.getInput(), dataGroup.getBlock());

    // build extractor
    final var channelReaders = buildExtractors(selector, input, dataGroup, channelGroup);

    return new ParallelRecordReaderImpl<>(
            ctx, channelReaders, rowDeserializer, dataList, channelGroup.getBlock());
  }

  private static Pair<DataGroup, ChannelGroup> selectChannels(
          LazyIoList<DataGroup> dataGroups, ChannelSelector selector)
          throws ChannelGroupNotFoundException, IOException {
    DataGroup dataGroup;
    ChannelGroup channelGroup;

    final var dataGroupsIter = dataGroups.iter();
    while ((dataGroup = dataGroupsIter.next()) != null) {
      final var channelGroupsIter = dataGroup.getChannelGroups().iter();
      while ((channelGroup = channelGroupsIter.next()) != null) {
        if (selector.selectGroup(dataGroup, channelGroup)) {
          if (dataGroup.getBlock().getRecordIdSize() != 0) {
            throw new NotImplementedFeatureException("Unsorted data groups not implemented");
          }

          if (!channelGroup.getBlock().getFlags().equals(ChannelGroupFlags.of(0))) {
            throw new NotImplementedFeatureException("Any channel group flags not implemented");
          }

          return Pair.of(dataGroup, channelGroup);
        }
      }
    }

    throw new ChannelGroupNotFoundException("No matching channel group found");
  }

  public static DataRead createSource(
          FileContext ctx, DataGroupBlock dataGroup) throws IOException {
    final var input = ctx.getInput();

    final var dataRoot = dataGroup.getData().resolve(DataRoot.META, input).orElse(null);
    if (dataRoot == null) {
      return new EmptyDataRead();
    } else if (dataRoot instanceof Data) {
      return new ByteBufferRead(ByteBuffer.wrap(((Data) dataRoot).getData()));
    } else if (dataRoot instanceof DataList) {
      return new DataListRead(input, (DataList) dataRoot);
    } else if (dataRoot instanceof HeaderList) {
      final var headerList = (HeaderList) dataRoot;

      if (headerList.getZipType() != ZipType.DEFLATE) {
        throw new NotImplementedFeatureException(
                "ZIP type not implemented: " + headerList.getZipType());
      }

      return headerList.getFirstDataList().resolve(DataList.META, input)
              .map(firstDataList -> (DataRead) new DataListRead(input, firstDataList))
              .orElseGet(EmptyDataRead::new);
    } else {
      throw new IllegalStateException("Should not happen");
    }
  }

  public static long[] collectDataList(
          ByteInput input, DataGroupBlock dataGroup) throws IOException {

    final var dataRoot = dataGroup.getData().resolve(DataRoot.META, input).orElse(null);
    if (dataRoot == null) {
      return new long[0];
    } else if (dataRoot instanceof Data) {
      return new long[]{dataGroup.getData().asLong()};
    } else if (dataRoot instanceof DataList) {
      return collectDataList(input, (DataList) dataRoot);
    } else if (dataRoot instanceof HeaderList) {
      final var headerList = (HeaderList) dataRoot;

      if (headerList.getZipType() != ZipType.DEFLATE) {
        throw new NotImplementedFeatureException(
                "ZIP type not implemented: " + headerList.getZipType());
      }

      final var firstDataList = headerList.getFirstDataList().resolve(DataList.META, input);
      return firstDataList.isPresent() ? collectDataList(input, firstDataList.get()) : new long[0];
    } else {
      throw new IllegalStateException("Should not happen");
    }
  }

  private static long[] collectDataList(
          ByteInput input, DataList dataList) throws IOException {
    final var res = new ArrayList<>(dataList.getData());
    while (!dataList.getNextDataList().isNil()) {
      dataList = dataList.getNextDataList().resolve(DataList.META, input).orElseThrow();
      res.addAll(dataList.getData());
    }
    return res.stream().mapToLong(Link::asLong).toArray();
  }

  private static ArrayList<ValueRead> buildExtractors(
          ChannelSelector selector, ByteInput input, DataGroup dataGroup,
          ChannelGroup channelGroup) throws IOException {
    final var dataGroupBlock = dataGroup.getBlock();
    final var channelGroupBlock = channelGroup.getBlock();
    log.finest(() ->
            "Record size: " + (dataGroupBlock.getRecordIdSize() + channelGroupBlock.getDataBytes()
                    + channelGroupBlock.getInvalidationBytes())
                    + " (RecordId: " + dataGroupBlock.getRecordIdSize() + ", Data: "
                    + channelGroupBlock.getDataBytes()
                    + ", InvalidationBytes: " + channelGroupBlock.getInvalidationBytes() + ")");

    final var channelReaders = new ArrayList<ValueRead>();
    final var iter = channelGroup.getChannels().iter();
    de.richardliebscher.mdf4.Channel ch;
    while ((ch = iter.next()) != null) {
      try {
        final var channelReader = createChannelReader(
                dataGroupBlock, channelGroupBlock, ch.getBlock(), input);
        if (selector.selectChannel(dataGroup, channelGroup, ch)) {
          channelReaders.add(channelReader);
        }
      } catch (NotImplementedFeatureException exception) {
        log.warning("Ignoring channel '" + ch.getBlock().getChannelName().resolve(Text.META, input)
                + "': " + exception.getMessage());
      }
    }

    return channelReaders;
  }
}
