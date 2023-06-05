/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public interface TextBased {

  static TextBased parse(ByteInput input) throws IOException {
    final var blockId = BlockType.parse(input);
    if (BlockType.MD.equals(blockId)) {
      return Metadata.parse(input);
    } else if (BlockType.TX.equals(blockId)) {
      return Text.parse(input);
    } else {
      throw new FormatException("Expected MD or TX block, bot got " + blockId);
    }
  }

  <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E;

  interface Visitor<R, E extends Throwable> {
    R visit(Text value) throws E;

    R visit(Metadata value) throws E;
  }

  Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Meta implements FromBytesInput<TextBased> {

    @Override
    public TextBased parse(ByteInput input) throws IOException {
      return TextBased.parse(input);
    }
  }
}
