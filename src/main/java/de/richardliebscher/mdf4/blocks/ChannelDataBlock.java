/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public interface ChannelDataBlock {

  ReadableByteChannel getChannel(ByteInput input) throws IOException;

  static ChannelDataBlock parse(ByteInput input) throws IOException {
    final var blockId = BlockType.parse(input);
    if (blockId.equals(BlockType.DT)) {
      return ChannelBlockData.parse(input);
    } else if (blockId.equals(BlockType.DZ)) {
      return ChannelBlockDataZipped.parse(input);
    } else {
      throw new NotImplementedFeatureException("Data block not implemented: " + blockId);
    }
  }

  Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Meta implements FromBytesInput<ChannelDataBlock> {

    @Override
    public ChannelDataBlock parse(ByteInput input) throws IOException {
      return ChannelDataBlock.parse(input);
    }
  }
}
