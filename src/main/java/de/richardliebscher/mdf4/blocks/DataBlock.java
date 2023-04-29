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

import java.io.IOException;

public interface DataBlock {

    static DataBlock parse(ByteInput input) throws IOException {
        final var blockId = BlockType.parse(input);
        if (blockId.equals(BlockType.DT)) {
            return Data.parse(input);
        } else if (blockId.equals(BlockType.DZ)) {
            return DataZipped.parse(input);
        } else {
            throw new NotImplementedFeatureException("Data block not implemented: " + blockId);
        }
    }

    Meta META = new Meta();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class Meta implements FromBytesInput<DataBlock> {
        @Override
        public DataBlock parse(ByteInput input) throws IOException {
            return DataBlock.parse(input);
        }
    }
}
