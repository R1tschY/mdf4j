/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Composition.
 *
 * <p>Structure or array</p>
 */
public interface Composition {

  static Composition parse(ByteInput input) throws IOException {
    final var blockId = BlockTypeId.parse(input);
    if (ChannelBlock.ID.equals(blockId)) {
      return ChannelBlock.parse(input);
      //} else if (ChannelArrayBlock.ID.equals(blockId)) {
      //  return ChannelArrayBlock.parse(input);
    } else {
      throw new FormatException("Expected CN or CA block, bot got " + blockId);
    }
  }

  Type TYPE = new Type();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Type implements BlockType<Composition> {

    @Override
    public BlockTypeId id() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Composition parse(ByteInput input) throws IOException {
      return Composition.parse(input);
    }
  }
}
