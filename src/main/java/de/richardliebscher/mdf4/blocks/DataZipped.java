/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

@Value
public class DataZipped implements DataRoot, DataBlock {

    BlockType originalBlockType;
    ZipType zipType;
    long zipParameter;
    long originalDataLength;
    @ToString.Exclude
    byte[] data;

    public static DataZipped parse(ByteInput input) throws IOException {
        BlockHeader.parseExpecting(BlockType.DZ, input, 0, 24);
        final var originalBlockType1 = input.readU8();
        final var originalBlockType2 = input.readU8();
        final var blockId = BlockType.of(originalBlockType1, originalBlockType2);
        final var zipType = ZipType.parse(input.readU8());
        input.skip(1);
        final var zipParameter = Integer.toUnsignedLong(input.readI32LE());
        final var originalDataLength = input.readI64LE();
        final var dataLength = input.readI64LE();
        final var data = input.readBytes(dataLength);
        return new DataZipped(blockId, zipType, zipParameter, originalDataLength, data);
    }

    public UncompressedData getUncompressed() throws IOException {
        try (var stream = createUncompressedStream(new ByteArrayInputStream(data))) {
            final var buffer = stream.readAllBytes(); // PERF: reduce copying
            if (originalBlockType.equals(BlockType.DT)) {
                return new Data(buffer);
            } else {
                throw new NotImplementedFeatureException(
                        "Uncompressed data block not implemented: " + originalBlockType);
            }
        }
    }

    private FilterInputStream createUncompressedStream(ByteArrayInputStream compressedStream)
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
    public static class Meta implements FromBytesInput<DataZipped> {
        @Override
        public DataZipped parse(ByteInput input) throws IOException {
            return DataZipped.parse(input);
        }
    }
}

