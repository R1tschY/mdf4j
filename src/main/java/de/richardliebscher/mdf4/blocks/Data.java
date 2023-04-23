/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.IOException;

@Value
public class Data implements DataRoot, DataBlock {

    byte[] data;

    public static Data parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parse(BlockId.DT, input);
        final var bytes = input.readBytes(blockHeader.getDataLength());
        return new Data(bytes);
    }

    public static final Meta META = new Meta();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Meta implements FromBytesInput<Data> {
        @Override
        public Data parse(ByteInput input) throws IOException {
            return Data.parse(input);
        }
    }
}

