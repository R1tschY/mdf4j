/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public interface DataRootBlock {

  static DataRootBlock parse(ByteInput input) throws IOException {
    final var blockId = BlockType.parse(input);
    if (blockId.equals(BlockType.DL)) {
      return DataListBlock.parse(input);
    } else if (blockId.equals(BlockType.DZ)) {
      return ChannelBlockDataZipped.parse(input);
    } else if (blockId.equals(BlockType.HL)) {
      return HeaderListBlock.parse(input);
    } else if (blockId.equals(BlockType.DT)) {
      return ChannelBlockData.parse(input);
    } else {
      throw new NotImplementedFeatureException("Root data block not implemented: " + blockId);
    }
  }

  Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Meta implements FromBytesInput<DataRootBlock> {

    @Override
    public DataRootBlock parse(ByteInput input) throws IOException {
      return DataRootBlock.parse(input);
    }
  }
}
