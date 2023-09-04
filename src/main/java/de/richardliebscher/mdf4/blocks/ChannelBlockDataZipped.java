/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.InflaterInputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class ChannelBlockDataZipped implements DataRootBlock, ChannelDataBlock {

  BlockType originalBlockType;
  ZipType zipType;
  long zipParameter;
  long originalDataLength;
  long dataPos;
  int dataLength;

  @Override
  public ReadableByteChannel getChannel(ByteInput input) throws IOException {
    input.seek(dataPos);
    return Channels.newChannel(createUncompressedStream(input.getStream()));
  }

  public static ChannelBlockDataZipped parse(ByteInput input) throws IOException {
    BlockHeader.parseExpecting(BlockType.DZ, input, 0, 24);
    final var originalBlockType1 = input.readU8();
    final var originalBlockType2 = input.readU8();
    final var blockId = BlockType.of(originalBlockType1, originalBlockType2);
    final var zipType = ZipType.parse(input.readU8());
    input.skip(1);
    final var zipParameter = Integer.toUnsignedLong(input.readI32());
    final var originalDataLength = input.readI64();
    final var dataLength = Math.toIntExact(input.readI64());
    return new ChannelBlockDataZipped(blockId, zipType, zipParameter, originalDataLength,
        input.pos(), dataLength);
  }

  private FilterInputStream createUncompressedStream(InputStream compressedStream)
      throws NotImplementedFeatureException {
    switch (getZipType()) {
      case DEFLATE:
        return new InflaterInputStream(compressedStream);
      case TRANSPOSITION_DEFLATE:
      default:
        throw new NotImplementedFeatureException("ZIP type not implemented: " + getZipType());
    }
  }

  public static final Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Meta implements FromBytesInput<ChannelBlockDataZipped> {

    @Override
    public ChannelBlockDataZipped parse(ByteInput input) throws IOException {
      return ChannelBlockDataZipped.parse(input);
    }
  }
}

