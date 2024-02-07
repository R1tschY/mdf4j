/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public interface ChannelDataBlock {

  ReadableByteChannel getChannel(ByteInput input) throws IOException;

  static ChannelDataBlock parse(ByteInput input) throws IOException {
    final var blockId = BlockTypeId.parse(input);
    if (blockId.equals(ChannelBlockData.ID)) {
      return ChannelBlockData.parse(input);
    } else if (blockId.equals(ChannelBlockDataZipped.ID)) {
      return ChannelBlockDataZipped.parse(input);
    } else {
      throw new NotImplementedFeatureException("Data block not implemented: " + blockId);
    }
  }

  Type TYPE = new Type();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Type implements BlockType<ChannelDataBlock> {

    @Override
    public BlockTypeId id() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ChannelDataBlock parse(ByteInput input) throws IOException {
      return ChannelDataBlock.parse(input);
    }
  }
}
