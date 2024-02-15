/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.impl;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.LazyIoList;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.ChannelBlock;
import de.richardliebscher.mdf4.blocks.ChannelBlock.Iterator;
import de.richardliebscher.mdf4.blocks.ChannelConversionBlock;
import de.richardliebscher.mdf4.blocks.ChannelDataType;
import de.richardliebscher.mdf4.blocks.ChannelFlag;
import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.blocks.Composition;
import de.richardliebscher.mdf4.blocks.DataBlock;
import de.richardliebscher.mdf4.blocks.DataGroupBlock;
import de.richardliebscher.mdf4.blocks.DataListBlock;
import de.richardliebscher.mdf4.blocks.DataStorage;
import de.richardliebscher.mdf4.blocks.HeaderListBlock;
import de.richardliebscher.mdf4.blocks.TextBlock;
import de.richardliebscher.mdf4.blocks.ZipType;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.extract.ParallelRecordReader;
import de.richardliebscher.mdf4.extract.RecordFactory;
import de.richardliebscher.mdf4.extract.SerializableRecordFactory;
import de.richardliebscher.mdf4.extract.SizedRecordReader;
import de.richardliebscher.mdf4.extract.de.Deserialize;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import de.richardliebscher.mdf4.extract.de.Deserializer;
import de.richardliebscher.mdf4.extract.de.StructAccess;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.extract.read.DataRead;
import de.richardliebscher.mdf4.extract.read.InvalidValueRead;
import de.richardliebscher.mdf4.extract.read.LinearConversion;
import de.richardliebscher.mdf4.extract.read.RationalConversion;
import de.richardliebscher.mdf4.extract.read.ReadInto;
import de.richardliebscher.mdf4.extract.read.ReadIntoFactory;
import de.richardliebscher.mdf4.extract.read.RecordBuffer;
import de.richardliebscher.mdf4.extract.read.ValueRead;
import de.richardliebscher.mdf4.extract.read.ValueReadFactory;
import de.richardliebscher.mdf4.internal.Arrays;
import de.richardliebscher.mdf4.internal.FileContext;
import de.richardliebscher.mdf4.internal.Pair;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecordReaderFactory {

  private static ValueReadFactory createChannelReaderFactory(
      DataGroupBlock dataGroup, ChannelGroupBlock group,
      ChannelBlock channelBlock, ByteInput input) throws IOException {
    if (channelBlock.getBitOffset() != 0) {
      throw new NotImplementedFeatureException("Non-zero bit offset is not implemented");
    }
    if (channelBlock.getFlags().isSet(ChannelFlag.ALL_VALUES_INVALID)) {
      return ValueReadFactory.of(new InvalidValueRead());
    }

    if (!channelBlock.getComposition().isNil()) {
      return createCompositionDataReader(dataGroup, group, channelBlock, input);
    }

    final ValueReadFactory rawValue;
    switch (channelBlock.getType()) {
      case FIXED_LENGTH_DATA_CHANNEL:
      case MASTER_CHANNEL:
      case SYNCHRONIZATION_CHANNEL:
        rawValue = createFixedLengthDataReader(channelBlock);
        break;
      case VARIABLE_LENGTH_DATA_CHANNEL:
        rawValue = createVariableLengthDataReader(channelBlock);
        break;
      case VIRTUAL_DATA_CHANNEL:
      case VIRTUAL_MASTER_CHANNEL:
        rawValue = createVirtualDataReader(channelBlock);
        break;
      case MAXIMUM_LENGTH_CHANNEL:
        rawValue = createMaxLengthDataReader(channelBlock, input);
        break;
      default:
        throw new NotImplementedFeatureException(
            "Channel type not implemented: " + channelBlock.getType());
    }

    final ValueReadFactory converted;
    final var channelConversion = channelBlock.getConversionRule()
        .resolve(ChannelConversionBlock.TYPE, input);
    if (channelConversion.isPresent()) {
      final var cc = channelConversion.get();
      switch (cc.getType()) {
        case IDENTITY:
          converted = rawValue;
          break;
        case LINEAR:
          converted = in -> new LinearConversion(cc, rawValue.build(in));
          break;
        case RATIONAL:
          converted = in -> new RationalConversion(cc, rawValue.build(in));
          break;
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
    } else {
      converted = rawValue;
    }

    if (channelBlock.getFlags().isSet(ChannelFlag.INVALIDATION_BIT_VALID)) {
      return createInvalidationReader(dataGroup, group, channelBlock, converted);
    } else {
      return converted;
    }
  }

  private static ValueReadFactory createCompositionDataReader(
      DataGroupBlock dataGroup, ChannelGroupBlock group,
      ChannelBlock frame, ByteInput input) throws IOException {
    final var composition = frame.getComposition().resolve(Composition.TYPE, input)
        .orElseThrow();
    final ChannelBlock firstField;
    if (composition instanceof ChannelBlock) {
      firstField = (ChannelBlock) composition;
    } else {
      throw new NotImplementedFeatureException("Reading arrays is not implemented");
    }

    final var fields = new ArrayList<ChannelBlock>();
    fields.add(firstField);
    final var fieldsIter = new Iterator(firstField.getNextChannel(), input);
    while (fieldsIter.hasNext()) {
      fields.add(fieldsIter.next());
    }

    if (frame.getDataType() != ChannelDataType.BYTE_ARRAY) {
      log.warning(() -> "Composition channel " + frame.getChannelName()
          + " has unexpected data type " + frame.getDataType());
    }

    switch (frame.getType()) {
      case FIXED_LENGTH_DATA_CHANNEL:
        return createFixedLengthStructureDataReader(dataGroup, group, frame, fields, input);
      case MASTER_CHANNEL:
      case SYNCHRONIZATION_CHANNEL:
      case VARIABLE_LENGTH_DATA_CHANNEL:
      case VIRTUAL_DATA_CHANNEL:
      case VIRTUAL_MASTER_CHANNEL:
      case MAXIMUM_LENGTH_CHANNEL:
      default:
        throw new NotImplementedFeatureException(
            "Channel type not implemented for composition: " + frame.getType());
    }
  }

  private static ValueReadFactory createFixedLengthStructureDataReader(
      DataGroupBlock dataGroup, ChannelGroupBlock group, ChannelBlock frame,
      List<ChannelBlock> fields, ByteInput input) throws IOException {

    final var readFieldFactories = new ArrayList<ValueReadFactory>(fields.size());
    for (int i = 0; i < fields.size(); i++) {
      final var field = fields.get(i);
      try {
        readFieldFactories.add(createChannelReaderFactory(dataGroup, group, field, input));
      } catch (NotImplementedFeatureException e) {
        log.warning("Ignoring field '" + getChannelName(field, input) + "' of channel '"
            + getChannelName(frame, input) + "': " + e.getMessage());
      }
    }

    return in -> {
      final var fieldReaders = new ValueRead[readFieldFactories.size()];
      for (int i = 0; i < readFieldFactories.size(); i++) {
        fieldReaders[i] = readFieldFactories.get(i).build(input);
      }

      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          return visitor.visitStruct(new StructAccess() {
            private int index = 0;

            @Override
            public int fields() {
              return fieldReaders.length;
            }

            @Override
            public <T1> T1 next_field(Deserialize<T1> deserialize) throws IOException {
              if (index >= fieldReaders.length) {
                throw new NoSuchElementException();
              }

              return deserialize.deserialize(new Deserializer() {
                @Override
                public <R> R deserialize_value(Visitor<R> visitor) throws IOException {
                  return fieldReaders[index++].read(input, visitor);
                }

                @Override
                public void ignore() {
                  index++;
                }
              });
            }
          });
        }
      };
    };
  }

  private static TextBlock getChannelName(ChannelBlock channel, ByteInput input)
      throws IOException {
    return channel.getChannelName().resolve(TextBlock.TYPE, input)
        .orElseThrow(() -> new FormatException("channel name required"));
  }

  private static ValueReadFactory createInvalidationReader(
      DataGroupBlock dataGroup, ChannelGroupBlock group, ChannelBlock channelBlock,
      ValueReadFactory valueReadFactory)
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
    return input -> {
      final var valueRead = valueReadFactory.build(input);
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
    };
  }

  private static ValueReadFactory createFixedLengthDataReader(ChannelBlock channelBlock)
      throws IOException {
    switch (channelBlock.getDataType()) {
      case UINT_LE:
        return ValueReadFactory.of(createUintLeRead(channelBlock));
      case UINT_BE:
        return ValueReadFactory.of(createUintBeRead(channelBlock));
      case INT_LE:
        return ValueReadFactory.of(createIntLeRead(channelBlock));
      case INT_BE:
        return ValueReadFactory.of(createIntBeRead(channelBlock));
      case FLOAT_LE:
        return ValueReadFactory.of(createFloatLeRead(channelBlock));
      case FLOAT_BE:
        return ValueReadFactory.of(createFloatBeRead(channelBlock));
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

  private static int getByteCount(ChannelBlock channelBlock, String message)
      throws FormatException {
    final var bitCount = channelBlock.getBitCount();
    if (bitCount % 8 != 0) {
      throw new FormatException(message);
    }
    return bitCount / 8;
  }

  private static ValueReadFactory createStringRead(ChannelBlock channelBlock, Charset charset)
      throws FormatException {
    final var byteOffset = channelBlock.getByteOffset();
    final var byteCount = getByteCount(channelBlock,
        "Bit count must be a multiple of 8 for string channels");

    return input -> {
      final var buffer = ThreadLocal.withInitial(() -> new byte[byteCount]);
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          final var buf = buffer.get();
          input.readBytes(byteOffset, buf, 0, buf.length);

          if (charset.equals(StandardCharsets.UTF_8) || charset.equals(
              StandardCharsets.ISO_8859_1)) {
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
    };
  }

  private static ValueReadFactory createByteArrayRead(ChannelBlock channelBlock)
      throws FormatException {
    final var byteOffset = channelBlock.getByteOffset();
    final var byteCount = getByteCount(channelBlock,
        "Bit count must be a multiple of 8 for byte array channels");

    return input -> {
      final var buffer = ThreadLocal.withInitial(() -> new byte[byteCount]);
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          final var dest = buffer.get();
          input.readBytes(byteOffset, dest, 0, dest.length);
          return visitor.visitByteArray(dest);
        }
      };
    };
  }

  private static String trimString(String data) throws IOException {
    final var size = data.indexOf('\0');
    if (size == -1) {
      throw new FormatException("Missing zero termination of string value");
    }
    return data.substring(0, size);
  }

  private static ValueReadFactory createVariableLengthDataReader(ChannelBlock channelBlock)
      throws IOException {
    switch (channelBlock.getDataType()) {
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
      default:
        throw new NotImplementedFeatureException(
            "Reading data type " + channelBlock.getDataType()
                + " with variable length not implemented");
    }
  }

  private static ValueReadFactory createVarcharRead(ChannelBlock channelBlock, Charset charset)
      throws FormatException {
    final var byteOffset = channelBlock.getByteOffset();
    final var byteCount = getByteCount(channelBlock,
        "Bit count must be a multiple of 8 for string channels");

    return input -> {
      final var buffer = ThreadLocal.withInitial(() -> new byte[byteCount]);
      final var sdInput = input.dup();
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          final var buf = buffer.get();
          input.readBytes(byteOffset, buf, 0, buf.length);

          if (charset.equals(StandardCharsets.UTF_8) || charset.equals(
              StandardCharsets.ISO_8859_1)) {
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
    };
  }

  private static ValueReadFactory createVirtualDataReader(ChannelBlock channelBlock)
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
    return ValueReadFactory.of(new ValueRead() {
      @Override
      public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
        return visitor.visitU64(input.getRecordIndex());
      }
    });
  }

  ////
  // Max length channels

  private static ValueReadFactory createMaxLengthDataReader(
      ChannelBlock channelBlock, ByteInput input) throws IOException {
    switch (channelBlock.getDataType()) {
      case STRING_LATIN1:
        return createMaxLenStringRead(channelBlock, StandardCharsets.ISO_8859_1, input);
      case STRING_UTF8:
        return createMaxLenStringRead(channelBlock, StandardCharsets.UTF_8, input);
      case STRING_UTF16LE:
        return createMaxLenStringRead(channelBlock, StandardCharsets.UTF_16LE, input);
      case STRING_UTF16BE:
        return createMaxLenStringRead(channelBlock, StandardCharsets.UTF_16BE, input);
      case BYTE_ARRAY:
        return createMaxLenByteArrayRead(channelBlock, input);
      case MIME_SAMPLE:
      case MIME_STREAM:
        throw new NotImplementedFeatureException(
            "Reading data type " + channelBlock.getDataType()
                + " not implemented for maximum length channel");
      default:
        throw new FormatException(
            "Data type " + channelBlock.getDataType()
                + " not allowed for maximum length channel");

    }
  }

  private static ValueReadFactory createSizeDataReader(ChannelBlock channelBlock, ByteInput input)
      throws IOException {
    final var sizeChannel = channelBlock.getMaxLengthChannel()
        .resolve(ChannelBlock.TYPE, input)
        .orElseThrow(() -> new FormatException("Maximum length channel requires cn_data link set"));

    if (!sizeChannel.getConversionRule().isNil()) {
      throw new NotImplementedFeatureException("Conversion rule for size channel not implemented");
    }

    switch (sizeChannel.getDataType()) {
      case UINT_LE:
        return ValueReadFactory.of(createUintLeRead(sizeChannel));
      case UINT_BE:
        return ValueReadFactory.of(createUintBeRead(sizeChannel));
      case INT_LE:
        return ValueReadFactory.of(createIntLeRead(sizeChannel));
      case INT_BE:
        return ValueReadFactory.of(createIntBeRead(sizeChannel));
      default:
        throw new NotImplementedFeatureException(
            "Reading data type " + sizeChannel.getDataType() + " as size not implemented");
    }
  }

  private static int readSize(
      ValueRead valueRead, RecordBuffer input, SizeVisitor sizeVisitor, byte[] buf)
      throws IOException {
    valueRead.read(input, sizeVisitor);

    final var size = sizeVisitor.getValue();
    if (size > buf.length) {
      throw new FormatException(
          "Size value bigger than maximum allowed size: " + size + " > " + buf.length);
    }
    return size;
  }

  private static ValueReadFactory createMaxLenStringRead(ChannelBlock channelBlock, Charset charset,
      ByteInput input) throws IOException {
    final var byteOffset = channelBlock.getByteOffset();
    final var byteCount = getByteCount(channelBlock,
        "Bit count must be a multiple of 8 for string channels");

    final var sizeReaderFactory = createSizeDataReader(channelBlock, input);

    return in -> {
      final var buffer = ThreadLocal.withInitial(
          () -> Pair.of(new byte[byteCount], new SizeVisitor()));
      final var sizeReader = sizeReaderFactory.build(in);
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          final var localCache = buffer.get();
          sizeReader.read(input, localCache.getRight());

          final var buf = localCache.getLeft();
          final var size = readSize(sizeReader, input, localCache.getRight(), buf);

          input.readBytes(byteOffset, buf, 0, size);
          if (charset.equals(StandardCharsets.UTF_8) || charset.equals(
              StandardCharsets.ISO_8859_1)) {
            final var trimmedSize = Arrays.indexOf(buf, 0, size, (byte) 0);
            if (trimmedSize == -1) {
              throw new FormatException("Missing zero termination of string value");
            }
            return visitor.visitString(trimString(new String(buf, 0, trimmedSize, charset)));
          } else {
            return visitor.visitString(trimString(new String(buf, charset)));
          }
        }
      };
    };
  }

  private static ValueReadFactory createMaxLenByteArrayRead(
      ChannelBlock channelBlock, ByteInput input) throws IOException {
    final var byteOffset = channelBlock.getByteOffset();
    final var byteCount = getByteCount(channelBlock,
        "Bit count must be a multiple of 8 for byte array channels");
    final var sizeReaderFactory = createSizeDataReader(channelBlock, input);

    return in -> {
      final var buffer = ThreadLocal.withInitial(
          () -> Pair.of(new byte[byteCount], new SizeVisitor()));
      final var sizeReader = sizeReaderFactory.build(in);
      return new ValueRead() {
        @Override
        public <T> T read(RecordBuffer input, Visitor<T> visitor) throws IOException {
          final var localCache = buffer.get();
          final var buf = localCache.getLeft();

          final var size = readSize(sizeReader, input, localCache.getRight(), buf);

          input.readBytes(byteOffset, buf, 0, size);
          return visitor.visitByteArray(buf, 0, size);
        }
      };
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

          return Pair.of(dataGroup, channelGroup);
        }
      }
    }

    throw new ChannelGroupNotFoundException("No matching channel group found");
  }

  public static DataRead<DataBlock> createSource(
      FileContext ctx, DataGroupBlock dataGroup) throws IOException {
    return DataRead.of(
        dataGroup.getData().resolve(DataBlock.CONTAINER_TYPE, ctx.getInput()).orElse(null),
        ctx.getInput(),
        DataBlock.STORAGE_TYPE);
  }

  public static Pair<long[], long[]> collectDataList(
      ByteInput input, DataGroupBlock dataGroup) throws IOException {

    final var dataRoot = dataGroup.getData().resolve(DataBlock.CONTAINER_TYPE, input).orElse(null);
    if (dataRoot == null) {
      return Pair.of(new long[0], new long[0]);
    } else if (dataRoot instanceof DataStorage) {
      return Pair.of(new long[]{dataGroup.getData().asLong()}, new long[]{0});
    } else if (dataRoot instanceof DataListBlock) {
      return collectDataList(input, (DataListBlock<DataBlock>) dataRoot);
    } else if (dataRoot instanceof HeaderListBlock) {
      final var headerList = (HeaderListBlock<DataBlock>) dataRoot;

      if (headerList.getZipType() != ZipType.DEFLATE) {
        throw new NotImplementedFeatureException(
            "ZIP type not implemented: " + headerList.getZipType());
      }

      final var firstDataList = headerList.getFirstDataList().resolve(DataListBlock.DT_TYPE, input);
      return firstDataList.isPresent()
          ? collectDataList(input, firstDataList.get())
          : Pair.of(new long[0], new long[0]);
    } else {
      throw new IllegalStateException("Should not happen");
    }
  }

  private static Pair<long[], long[]> collectDataList(
      ByteInput input, DataListBlock<DataBlock> dataList) throws IOException {
    final var dataLinks = new ArrayList<>(dataList.getData());
    final var offsetsList = new ArrayList<Long>();
    addOffsets(offsetsList, dataList);

    while (!dataList.getNextDataList().isNil()) {
      dataList = dataList.getNextDataList().resolve(DataListBlock.DT_TYPE, input).orElseThrow();
      dataLinks.addAll(dataList.getData());
      addOffsets(offsetsList, dataList);
    }

    return Pair.of(
        dataLinks.stream().mapToLong(Link::asLong).toArray(),
        offsetsList.stream().mapToLong(Long::longValue).toArray());
  }

  private static void addOffsets(List<Long> offsetsList, DataListBlock<DataBlock> dataList) {
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
        final var deserializeInto = selector.selectChannel(dataGroup, channelGroup, ch);
        if (deserializeInto != null) {
          final var channelReaderFactory = createChannelReaderFactory(
              dataGroupBlock, channelGroupBlock, ch.getBlock(), input);
          final var channelReader = channelReaderFactory.build(input);
          channels.add(ch);
          channelReaders.add(new ReadIntoImpl<>(deserializeInto, channelReader));
        }
      } catch (NotImplementedFeatureException exception) {
        log.warning("Ignoring channel '" + ch.getName() + "': " + exception.getMessage());
      }
    }

    return Pair.of(channelReaders, channels);
  }

  private static <B, R> List<ReadIntoFactory<B>> buildExtractors(
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

    final var channelReaders = new ArrayList<ReadIntoFactory<B>>();
    final var iter = channelGroup.getChannels().iter();
    de.richardliebscher.mdf4.Channel ch;
    while ((ch = iter.next()) != null) {
      try {
        final var deserializeInto = selector.selectChannel(dataGroup, channelGroup, ch);
        if (deserializeInto != null) {
          final var channelReaderFactory = createChannelReaderFactory(
              dataGroupBlock, channelGroupBlock, ch.getBlock(), input);
          channelReaders.add(
              in -> new ReadIntoImpl<>(deserializeInto, channelReaderFactory.build(input)));
        }
      } catch (NotImplementedFeatureException exception) {
        log.warning("Ignoring channel '" + ch.getName() + "': " + exception.getMessage());
      }
    }

    return channelReaders;
  }

  private static class ReadIntoImpl<B> implements ReadInto<B> {

    private final DeserializeInto<B> deserializeInto;
    private final ValueRead channelReader;

    public ReadIntoImpl(DeserializeInto<B> deserializeInto, ValueRead channelReader) {
      this.deserializeInto = deserializeInto;
      this.channelReader = channelReader;
    }

    @Override
    public void readInto(RecordBuffer recordBuffer, B destination) throws IOException {
      deserializeInto.deserializeInto(new Deserializer() {
        @Override
        public <R2> R2 deserialize_value(Visitor<R2> visitor) throws IOException {
          return channelReader.read(recordBuffer, visitor);
        }

        @Override
        public void ignore() {
          // noop
        }
      }, destination);
    }
  }
}
