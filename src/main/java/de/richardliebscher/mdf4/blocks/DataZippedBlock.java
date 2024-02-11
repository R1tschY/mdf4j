/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.InflaterInputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class DataZippedBlock<T extends Data<T>> implements DataContainer<T>, DataStorage<T> {

  BlockTypeId originalBlockTypeId;
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

  @Override
  public long getChannelLength() {
    return originalDataLength;
  }

  public static <T extends Data<T>> DataZippedBlock<T> parse(ByteInput input)
      throws IOException {
    BlockHeader.parseExpecting(ID, input, 0, 24);
    final var originalBlockType1 = input.readU8();
    final var originalBlockType2 = input.readU8();
    final var blockId = BlockTypeId.of(originalBlockType1, originalBlockType2);
    final var zipType = ZipType.parse(input.readU8());
    input.skip(1);
    final var zipParameter = Integer.toUnsignedLong(input.readI32());
    final var originalDataLength = input.readI64();
    final var dataLength = Math.toIntExact(input.readI64());
    return new DataZippedBlock<>(blockId, zipType, zipParameter, originalDataLength,
        input.pos(), dataLength);
  }

  private InputStream createUncompressedStream(InputStream compressedStream) throws IOException {
    switch (getZipType()) {
      case DEFLATE:
        return new InflaterInputStream(compressedStream);
      case TRANSPOSITION_DEFLATE:
        return transposed(zipParameter, new InflaterInputStream(compressedStream));
      default:
        throw new NotImplementedFeatureException(
            "ZIP type not implemented: " + getZipType().getName());
    }
  }

  private static InputStream transposed(long columnSize, InputStream in) throws IOException {
    final var bytes = in.readAllBytes();

    final var n = Math.toIntExact(columnSize);
    final var m = bytes.length / columnSize;

    final var result = new byte[bytes.length];
    int k = 0;
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < m; j++, k++) {
        result[j * n + i] = bytes[k];
      }
    }
    System.arraycopy(bytes, k, result, k, bytes.length - k);

    return new ByteArrayInputStream(result);
  }

  public static final Type<DataBlock> DT_TYPE = new Type<>();
  public static final Type<SignalDataBlock> SD_TYPE = new Type<>();
  public static final BlockTypeId ID = BlockTypeId.of('D', 'Z');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type<T extends Data<T>> implements
      DataContainerType<T, DataZippedBlock<T>> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public DataZippedBlock<T> parse(ByteInput input) throws IOException {
      return DataZippedBlock.parse(input);
    }
  }
}

