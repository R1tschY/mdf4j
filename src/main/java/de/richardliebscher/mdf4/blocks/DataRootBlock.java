/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public interface DataRootBlock {

  static DataRootBlock parse(ByteInput input) throws IOException {
    final var blockId = BlockTypeId.parse(input);
    if (blockId.equals(DataListBlock.ID)) {
      return DataListBlock.parse(input);
    } else if (blockId.equals(ChannelBlockDataZipped.ID)) {
      return ChannelBlockDataZipped.parse(input);
    } else if (blockId.equals(HeaderListBlock.ID)) {
      return HeaderListBlock.parse(input);
    } else if (blockId.equals(ChannelBlockData.ID)) {
      return ChannelBlockData.parse(input);
    } else {
      throw new NotImplementedFeatureException("Root data block not implemented: " + blockId);
    }
  }

  Type TYPE = new Type();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Type implements BlockType<DataRootBlock> {

    @Override
    public BlockTypeId id() {
      throw new UnsupportedOperationException();
    }

    @Override
    public DataRootBlock parse(ByteInput input) throws IOException {
      return DataRootBlock.parse(input);
    }
  }
}
