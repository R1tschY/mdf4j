/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.impl;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.LazyIoList;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.ChannelBlock;
import de.richardliebscher.mdf4.blocks.ChannelBlockData;
import de.richardliebscher.mdf4.blocks.ChannelConversionBlock;
import de.richardliebscher.mdf4.blocks.ChannelFlag;
import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.blocks.DataGroupBlock;
import de.richardliebscher.mdf4.blocks.DataListBlock;
import de.richardliebscher.mdf4.blocks.DataRootBlock;
import de.richardliebscher.mdf4.blocks.HeaderListBlock;
import de.richardliebscher.mdf4.blocks.ZipType;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.extract.ParallelRecordReader;
import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.SerializableRecordFactory;
import de.richardliebscher.mdf4.extract.SizedRecordReader;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.InvalidDeserializer;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.extract.read.DataBlockRead;
import de.richardliebscher.mdf4.extract.read.DataListRead;
import de.richardliebscher.mdf4.extract.read.DataRead;
import de.richardliebscher.mdf4.extract.read.EmptyDataRead;
import de.richardliebscher.mdf4.extract.read.LinearConversion;
import de.richardliebscher.mdf4.extract.read.NoValueRead;
import de.richardliebscher.mdf4.extract.read.ReadInto;
import de.richardliebscher.mdf4.extract.read.RecordBuffer;
import de.richardliebscher.mdf4.extract.read.SerializableReadInto;
import de.richardliebscher.mdf4.extract.read.ValueRead;
import de.richardliebscher.mdf4.internal.Arrays;
import de.richardliebscher.mdf4.internal.FileContext;
import de.richardliebscher.mdf4.internal.Pair;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecordReaderFactory {

  private static ValueRead createChannelReader(
      DataGroupBlock dataGroup, ChannelGroupBlock group,
      ChannelBlock channelBlock, ByteInput input) throws IOException {
    if (channelBlock.getBitOffset() != 0) {
      throw new NotImplementedFeatureException("Non-zero bit offset is not implemented");
    }
    if (channelBlock.getFlags().isSet(ChannelFlag.ALL_VALUES_INVALID)) {
      return new NoValueRead(new InvalidDeserializer());
    }

    ValueRead valueRead;
    switch (channelBlock.getType()) {
      case FIXED_LENGTH_DATA_CHANNEL:
      case MASTER_CHANNEL:
      case SYNCHRONIZATION_CHANNEL:
        valueRead = createFixedLengthDataReader(channelBlock);
        break;
      case VIRTUAL_DATA_CHANNEL:
      case VIRTUAL_MASTER_CHANNEL:
        valueRead = createVirtualDataReader(channelBlock);
        break;
      case VARIABLE_LENGTH_DATA_CHANNEL:
      case MAXIMUM_LENGTH_CHANNEL:
      default:
        throw new NotImplementedFeatureException(
            "Channel type not implemented: " + channelBlock.getType());
    }

    final var channelConversion = channelBlock.getConversionRule()
        .resolve(ChannelConversionBlock.TYPE, input);
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

    if (channelBlock.getFlags().isSet(ChannelFlag.INVALIDATION_BIT_VALID)) {
      valueRead = createInvalidationReader(dataGroup, group, channelBlock, valueRead);
    }

    return valueRead;
  }

  private static ValueRead createInvalidationReader(
      DataGroupBlock dataGroup, ChannelGroupBlock group, ChannelBlock channelBlock,
      ValueRead valueRead)
      throws FormatException {
    final var groupBits = group.getInvalidationBytes() * 8;
    final var invalidationBit = channelBlock.getInvalidationBit();
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

  private static ValueRead createFixedLengthDataReader(ChannelBlock channelBlock)
      throws IOException {
    switch (channelBlock.getDataType()) {
      case UINT_LE:
        return createUintLeRead(channelBlock);
      case UINT_BE:
        return createUintBeRead(channelBlock);
      case INT_LE:
        return createIntLeRead(channelBlock);
      case INT_BE:
        return createIntBeRead(channelBlock);
      case FLOAT_LE:
        return createFloatLeRead(channelBlock);
      case FLOAT_BE:
        return createFloatBeRead(channelBlock);
      case STRING_LATIN1:
        return createStringRead(channelBlock, StandardCharsets.ISO_8859_1);
      case STRING_UTF8:
        return createStringRead(channelBlock, StandardCharsets.UTF_8);
      case STRING_UTF16LE:
        return createStringRead(channelBlock, StandardCharsets.UTF_16LE);
      case STRING_UTF16BE:
        return createStringRead(channelBlock, StandardCharsets.UTF_16BE);
      case BYTE_ARRAY:
        return createByteArrayRead(channelBlock);
      case MIME_SAMPLE:
      case MIME_STREAM:
      case CANOPEN_DATE:
      case CANOPEN_TIME:
      case COMPLEX_LE:
      case COMPLEX_BE:
      default:
        throw new NotImplementedFeatureException(
            "Reading data type " + channelBlock.getDataType() + " not implemented");
    }
  }

  private static ValueRead createUintLeRead(ChannelBlock channelBlock)
      throws NotImplementedFeatureException {
    final var byteOffset = channelBlock.getByteOffset();
    switch (channelBlock.getBitCount()) {
      case 8:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitU8(input.readU8(byteOffset));
          }
        };
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitU16(input.readI16Le(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitU32(input.readI32Le(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitU64(input.readI64Le(byteOffset));
          }
        };
      default:
    }

    if (channelBlock.getBitCount() < 8) {
      return createSmallUnsignedRead(channelBlock, byteOffset);
    } else if (channelBlock.getBitCount() < 16) {
      final var mask = (1 << channelBlock.getBitCount()) - 1;
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitU16((short) (input.readI16Le(byteOffset) & mask));
        }
      };
    } else if (channelBlock.getBitCount() < 32) {
      final var mask = (1 << channelBlock.getBitCount()) - 1;
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitU32(input.readI32Le(byteOffset) & mask);
        }
      };
    } else if (channelBlock.getBitCount() < 64) {
      final var mask = (1L << channelBlock.getBitCount()) - 1L;
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitU64(input.readI64Le(byteOffset) & mask);
        }
      };
    } else {
      throw new NotImplementedFeatureException(
          "Integer with more than 64 bits are not implemented, got " + channelBlock.getBitCount());
    }
  }

  private static ValueRead createUintBeRead(ChannelBlock channelBlock)
      throws NotImplementedFeatureException {
    final var byteOffset = channelBlock.getByteOffset();
    switch (channelBlock.getBitCount()) {
      case 8:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitU8(input.readU8(byteOffset));
          }
        };
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitU16(input.readI16Be(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitU32(input.readI32Be(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitU64(input.readI64Be(byteOffset));
          }
        };
      default:
    }

    if (channelBlock.getBitCount() < 8) {
      return createSmallUnsignedRead(channelBlock, byteOffset);
    } else if (channelBlock.getBitCount() < 16) {
      final var mask = (1 << channelBlock.getBitCount()) - 1;
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitU16((short) (input.readI16Be(byteOffset) & mask));
        }
      };
    } else if (channelBlock.getBitCount() < 32) {
      final var mask = (1 << channelBlock.getBitCount()) - 1;
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitU32(input.readI32Be(byteOffset) & mask);
        }
      };
    } else if (channelBlock.getBitCount() < 64) {
      final var mask = (1L << channelBlock.getBitCount()) - 1L;
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitU64(input.readI64Be(byteOffset) & mask);
        }
      };
    } else {
      throw new NotImplementedFeatureException(
          "Integer with more than 64 bits are not implemented, got " + channelBlock.getBitCount());
    }
  }

  private static ValueRead createSmallUnsignedRead(ChannelBlock channelBlock, int byteOffset) {
    final var mask = (1 << channelBlock.getBitCount()) - 1;
    return new ValueRead() {
      @Override
      public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
        return visitor.visitU8((byte) (input.readU8(byteOffset) & mask));
      }
    };
  }

  private static ValueRead createIntLeRead(ChannelBlock channelBlock)
      throws NotImplementedFeatureException {
    final var byteOffset = channelBlock.getByteOffset();
    switch (channelBlock.getBitCount()) {
      case 8:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitI8(input.readU8(byteOffset));
          }
        };
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitI16(input.readI16Le(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitI32(input.readI32Le(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitI64(input.readI64Le(byteOffset));
          }
        };
      default:
    }

    if (channelBlock.getBitCount() < 8) {
      return createSmallIntegerRead(channelBlock, byteOffset);
    } else if (channelBlock.getBitCount() < 16) {
      final var unusedBits = 32 - channelBlock.getBitCount();
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitI16(
              (short) (input.readI16Le(byteOffset) << unusedBits >> unusedBits));
        }
      };
    } else if (channelBlock.getBitCount() < 32) {
      final var unusedBits = 32 - channelBlock.getBitCount();
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitI32(input.readI32Le(byteOffset) << unusedBits >> unusedBits);
        }
      };
    } else if (channelBlock.getBitCount() < 64) {
      final var unusedBits = 64 - channelBlock.getBitCount();
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitI64(
              input.readI64Le(byteOffset) << unusedBits >> unusedBits);
        }
      };
    } else {
      throw new NotImplementedFeatureException(
          "Integer with more than 64 bits are not implemented, got " + channelBlock.getBitCount());
    }
  }

  private static ValueRead createIntBeRead(ChannelBlock channelBlock)
      throws NotImplementedFeatureException {
    final var byteOffset = channelBlock.getByteOffset();
    switch (channelBlock.getBitCount()) {
      case 8:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitI8(input.readU8(byteOffset));
          }
        };
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitI16(input.readI16Be(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitI32(input.readI32Be(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitI64(input.readI64Be(byteOffset));
          }
        };
      default:
    }

    if (channelBlock.getBitCount() < 8) {
      return createSmallIntegerRead(channelBlock, byteOffset);
    } else if (channelBlock.getBitCount() < 16) {
      final var unusedBits = 32 - channelBlock.getBitCount();
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitI16(
              (short) (input.readI16Be(byteOffset) << unusedBits >> unusedBits));
        }
      };
    } else if (channelBlock.getBitCount() < 32) {
      final var unusedBits = 32 - channelBlock.getBitCount();
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitI32(input.readI32Be(byteOffset) << unusedBits >> unusedBits);
        }
      };
    } else if (channelBlock.getBitCount() < 64) {
      final var unusedBits = 64 - channelBlock.getBitCount();
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitI64(
              input.readI64Be(byteOffset) << unusedBits >> unusedBits);
        }
      };
    } else {
      throw new NotImplementedFeatureException(
          "Integer with more than 64 bits are not implemented, got " + channelBlock.getBitCount());
    }
  }

  private static ValueRead createSmallIntegerRead(ChannelBlock channelBlock, int byteOffset) {
    final var unusedBits = 32 - channelBlock.getBitCount();
    return new ValueRead() {
      @Override
      public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
        return visitor.visitI8(
            (byte) (input.readU8(byteOffset) << unusedBits >> unusedBits));
      }
    };
  }

  private static ValueRead createFloatLeRead(ChannelBlock channelBlock)
      throws NotImplementedFeatureException {
    final var byteOffset = channelBlock.getByteOffset();
    switch (channelBlock.getBitCount()) {
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitF16(input.readI16Le(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitF32(input.readF32Le(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitF64(input.readF64Le(byteOffset));
          }
        };
      default:
        throw new NotImplementedFeatureException(
            "Only floating point numbers with 32 or 64 bits are implemented, got "
                + channelBlock.getBitCount() + " bits");
    }
  }

  private static ValueRead createFloatBeRead(ChannelBlock channelBlock)
      throws NotImplementedFeatureException {
    final var byteOffset = channelBlock.getByteOffset();
    switch (channelBlock.getBitCount()) {
      case 16:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitF16(input.readI16Be(byteOffset));
          }
        };
      case 32:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitF32(input.readF32Be(byteOffset));
          }
        };
      case 64:
        return new ValueRead() {
          @Override
          public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
            return visitor.visitF64(input.readF64Be(byteOffset));
          }
        };
      default:
        throw new NotImplementedFeatureException(
            "Only floating point numbers with 4 or 8 bytes are implemented");
    }
  }

  private static ValueRead createStringRead(ChannelBlock channelBlock, Charset charset)
      throws FormatException {
    final var byteOffset = channelBlock.getByteOffset();
    final var bitCount = channelBlock.getBitCount();
    if (bitCount % 8 != 0) {
      throw new FormatException("Bit count must be a multiple of 8 for string channels");
    }
    final var byteCount = bitCount / 8;

    return new ValueRead() {
      private transient ThreadLocal<byte[]> buffer = ThreadLocal.withInitial(
          () -> new byte[byteCount]);

      private void readObject(ObjectInputStream inputStream)
          throws ClassNotFoundException, IOException {
        inputStream.defaultReadObject();
        buffer = ThreadLocal.withInitial(() -> new byte[byteCount]);
      }

      @Override
      public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
        final var buf = buffer.get();
        input.readBytes(byteOffset, buf);

        if (charset.equals(StandardCharsets.UTF_8) || charset.equals(StandardCharsets.ISO_8859_1)) {
          final var size = Arrays.indexOf(buf, (byte) 0);
          if (size == -1) {
            throw new FormatException("Missing zero termination of string value");
          }
          return visitor.visitString(trimString(new String(buf, 0, size, charset)));
        } else {
          return visitor.visitString(trimString(new String(buf, charset)));
        }
      }
    };
  }

  private static ValueRead createByteArrayRead(ChannelBlock channelBlock)
      throws FormatException {
    final var byteOffset = channelBlock.getByteOffset();
    final var bitCount = channelBlock.getBitCount();
    if (bitCount % 8 != 0) {
      throw new FormatException("Bit count must be a multiple of 8 for byte array channels");
    }
    final var byteCount = bitCount / 8;

    return new ValueRead() {
      private transient ThreadLocal<byte[]> buffer = ThreadLocal.withInitial(
          () -> new byte[byteCount]);

      private void readObject(ObjectInputStream inputStream)
          throws ClassNotFoundException, IOException {
        inputStream.defaultReadObject();
        buffer = ThreadLocal.withInitial(() -> new byte[byteCount]);
      }

      @Override
      public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
        final var dest = buffer.get();
        input.readBytes(byteOffset, dest);
        return visitor.visitByteArray(ByteBuffer.wrap(dest));
      }
    };
  }

  private static String trimString(String data) throws IOException {
    final var size = data.indexOf('\0');
    if (size == -1) {
      throw new FormatException("Missing zero termination of string value");
    }
    return data.substring(0, size);
  }

  private static ValueRead createVirtualDataReader(ChannelBlock channelBlock)
      throws FormatException {
    if (channelBlock.getBitCount() != 0) {
      throw new FormatException("Bit count of virtual master channel must be zero, but got "
          + channelBlock.getBitCount());
    }
    // TODO
    // if (channelBlock.getDataType() != ChannelDataType.UINT_LE) {
    //     throw new FormatException("Channel type of virtual master channel must be little endian
    //     unsigned integer, but got "
    //             + channelBlock.getDataType());
    // }
    // TODO: apply offset from HD block
    return new ValueRead() {
      @Override
      public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
        return visitor.visitU64(input.getRecordIndex());
      }
    };
  }

  /**
   * Internal API: Use {@link de.richardliebscher.mdf4.Mdf4File#newRecordReader}.
   *
   * @see de.richardliebscher.mdf4.Mdf4File#newRecordReader
   */
  public static <B, R> SizedRecordReader<B, R> createFor(FileContext ctx,
      LazyIoList<DataGroup> dataGroups, RecordFactory<B, R> factory)
      throws ChannelGroupNotFoundException, IOException {
    final var input = ctx.getInput();

    // select
    final var group = selectChannels(dataGroups, factory);
    final var dataGroup = group.getLeft();
    final var channelGroup = group.getRight();

    // data source
    final var source = createSource(ctx, dataGroup.getBlock());

    // build extractor
    final var channelReaders = buildExtractors(factory, input, dataGroup, channelGroup);

    return new DefaultRecordReader<>(
        channelReaders.getRight(), channelReaders.getLeft(), factory, source,
        channelGroup.getBlock());
  }

  public static <B, R> ParallelRecordReader<B, R> createParallelFor(
      FileContext ctx, LazyIoList<DataGroup> dataGroups,
      SerializableRecordFactory<B, R> recordFactory)
      throws ChannelGroupNotFoundException, IOException {
    final var input = ctx.getInput();

    // select
    final var group = selectChannels(dataGroups, recordFactory);
    final var dataGroup = group.getLeft();
    final var channelGroup = group.getRight();

    // data source
    final var dataListAndOffsets = collectDataList(ctx.getInput(), dataGroup.getBlock());

    // build extractor
    final var channelReaders = buildExtractors(recordFactory, input, dataGroup, channelGroup);

    return new DefaultParallelRecordReader<>(
        ctx, channelReaders, recordFactory,
        dataListAndOffsets.getLeft(), dataListAndOffsets.getRight(),
        channelGroup.getBlock());
  }

  private static Pair<DataGroup, ChannelGroup> selectChannels(
      LazyIoList<DataGroup> dataGroups, RecordFactory<?, ?> selector)
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

          if (!channelGroup.getBlock().getFlags().isEmpty()) {
            throw new NotImplementedFeatureException(
                "Some channel group flags not implemented: " + channelGroup.getBlock().getFlags());
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

    final var dataRoot = dataGroup.getData().resolve(DataRootBlock.TYPE, input).orElse(null);
    if (dataRoot == null) {
      return new EmptyDataRead();
    } else if (dataRoot instanceof ChannelBlockData) {
      return new DataBlockRead(input, (ChannelBlockData) dataRoot);
    } else if (dataRoot instanceof DataListBlock) {
      return new DataListRead(input, (DataListBlock) dataRoot);
    } else if (dataRoot instanceof HeaderListBlock) {
      final var headerList = (HeaderListBlock) dataRoot;

      if (headerList.getZipType() != ZipType.DEFLATE) {
        throw new NotImplementedFeatureException(
            "ZIP type not implemented: " + headerList.getZipType());
      }

      return headerList.getFirstDataList().resolve(DataListBlock.TYPE, input)
          .map(firstDataList -> (DataRead) new DataListRead(input, firstDataList))
          .orElseGet(EmptyDataRead::new);
    } else {
      throw new IllegalStateException("Should not happen");
    }
  }

  public static Pair<long[], long[]> collectDataList(
      ByteInput input, DataGroupBlock dataGroup) throws IOException {

    final var dataRoot = dataGroup.getData().resolve(DataRootBlock.TYPE, input).orElse(null);
    if (dataRoot == null) {
      return Pair.of(new long[0], new long[0]);
    } else if (dataRoot instanceof ChannelBlockData) {
      return Pair.of(new long[]{dataGroup.getData().asLong()}, new long[]{0});
    } else if (dataRoot instanceof DataListBlock) {
      return collectDataList(input, (DataListBlock) dataRoot);
    } else if (dataRoot instanceof HeaderListBlock) {
      final var headerList = (HeaderListBlock) dataRoot;

      if (headerList.getZipType() != ZipType.DEFLATE) {
        throw new NotImplementedFeatureException(
            "ZIP type not implemented: " + headerList.getZipType());
      }

      final var firstDataList = headerList.getFirstDataList().resolve(DataListBlock.TYPE, input);
      return firstDataList.isPresent()
          ? collectDataList(input, firstDataList.get())
          : Pair.of(new long[0], new long[0]);
    } else {
      throw new IllegalStateException("Should not happen");
    }
  }

  private static Pair<long[], long[]> collectDataList(
      ByteInput input, DataListBlock dataList) throws IOException {
    final var dataLinks = new ArrayList<>(dataList.getData());
    final var offsetsList = new ArrayList<Long>();
    addOffsets(offsetsList, dataList);

    while (!dataList.getNextDataList().isNil()) {
      dataList = dataList.getNextDataList().resolve(DataListBlock.TYPE, input).orElseThrow();
      dataLinks.addAll(dataList.getData());
      addOffsets(offsetsList, dataList);
    }

    return Pair.of(
        dataLinks.stream().mapToLong(Link::asLong).toArray(),
        offsetsList.stream().mapToLong(Long::longValue).toArray());
  }

  private static void addOffsets(List<Long> offsetsList, DataListBlock dataList) {
    dataList.getOffsetInfo().accept(
        new de.richardliebscher.mdf4.blocks.LengthOrOffsets.Visitor<
            List<Long>, RuntimeException>() {
          @Override
          public List<Long> visitLength(long length) {
            for (int i = 0; i < dataList.getData().size(); i++) {
              if (offsetsList.isEmpty()) {
                offsetsList.add(0L);
              } else {
                offsetsList.add(offsetsList.get(offsetsList.size() - 1) + length);
              }
            }
            return null;
          }

          @Override
          public List<Long> visitOffsets(long[] offsets) {
            for (long offset : offsets) {
              offsetsList.add(offset);
            }
            return null;
          }
        });
  }

  private static <B, R> Pair<List<ReadInto<B>>, List<Channel>> buildExtractors(
      RecordFactory<B, R> selector, ByteInput input, DataGroup dataGroup,
      ChannelGroup channelGroup) throws IOException {
    final var dataGroupBlock = dataGroup.getBlock();
    final var channelGroupBlock = channelGroup.getBlock();
    log.finest(() ->
        "Record size: " + (dataGroupBlock.getRecordIdSize() + channelGroupBlock.getDataBytes()
            + channelGroupBlock.getInvalidationBytes())
            + " (RecordId: " + dataGroupBlock.getRecordIdSize() + ", Data: "
            + channelGroupBlock.getDataBytes()
            + ", InvalidationBytes: " + channelGroupBlock.getInvalidationBytes() + ")");

    final var channels = new ArrayList<Channel>();
    final var channelReaders = new ArrayList<ReadInto<B>>();
    final var iter = channelGroup.getChannels().iter();
    Channel ch;
    while ((ch = iter.next()) != null) {
      try {
        final var channelReader = createChannelReader(
            dataGroupBlock, channelGroupBlock, ch.getBlock(), input);
        final var deserializeInto = selector.selectChannel(dataGroup, channelGroup, ch);
        if (deserializeInto != null) {
          channels.add(ch);
          channelReaders.add((recordBuffer, destination) -> {
            deserializeInto.deserializeInto(new Deserializer() {
              @Override
              public <R2> R2 deserialize_value(Visitor<R2> visitor) throws IOException {
                return channelReader.read(recordBuffer, visitor);
              }
            }, destination);
          });
        }
      } catch (NotImplementedFeatureException exception) {
        log.warning("Ignoring channel '" + ch.getName() + "': " + exception.getMessage());
      }
    }

    return Pair.of(channelReaders, channels);
  }

  private static <B, R> List<SerializableReadInto<B>> buildExtractors(
      SerializableRecordFactory<B, R> selector, ByteInput input, DataGroup dataGroup,
      ChannelGroup channelGroup) throws IOException {
    final var dataGroupBlock = dataGroup.getBlock();
    final var channelGroupBlock = channelGroup.getBlock();
    log.finest(() ->
        "Record size: " + (dataGroupBlock.getRecordIdSize() + channelGroupBlock.getDataBytes()
            + channelGroupBlock.getInvalidationBytes())
            + " (RecordId: " + dataGroupBlock.getRecordIdSize() + ", Data: "
            + channelGroupBlock.getDataBytes()
            + ", InvalidationBytes: " + channelGroupBlock.getInvalidationBytes() + ")");

    final var channelReaders = new ArrayList<SerializableReadInto<B>>();
    final var iter = channelGroup.getChannels().iter();
    de.richardliebscher.mdf4.Channel ch;
    while ((ch = iter.next()) != null) {
      try {
        final var channelReader = createChannelReader(
            dataGroupBlock, channelGroupBlock, ch.getBlock(), input);
        final var deserializeInto = selector.selectChannel(dataGroup, channelGroup, ch);
        if (deserializeInto != null) {
          channelReaders.add((recordBuffer, destination) -> {
            deserializeInto.deserializeInto(new Deserializer() {
              @Override
              public <R2> R2 deserialize_value(Visitor<R2> visitor) throws IOException {
                return channelReader.read(recordBuffer, visitor);
              }
            }, destination);
          });
        }
      } catch (NotImplementedFeatureException exception) {
        log.warning("Ignoring channel '" + ch.getName() + "': " + exception.getMessage());
      }
    }

    return channelReaders;
  }
}
