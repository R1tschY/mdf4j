package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.blocks.*;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.extract.de.InvalidDeserializer;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.extract.de.Visitor;
import de.richardliebscher.mdf4.extract.read.*;
import de.richardliebscher.mdf4.internal.InternalReader;
import de.richardliebscher.mdf4.internal.Pair;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import static de.richardliebscher.mdf4.internal.Iterators.asIterable;
import static de.richardliebscher.mdf4.internal.Iterators.streamBlockSeq;

@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RecordReaderFactory {
  private static ValueRead createChannelReader(DataGroup dataGroup, ChannelGroup group,
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
          DataGroup dataGroup, ChannelGroup group, Channel channel, ValueRead valueRead)
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
            .orElseThrow(() ->
                    new ChannelGroupNotFoundException("No matching channel group found"));

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
