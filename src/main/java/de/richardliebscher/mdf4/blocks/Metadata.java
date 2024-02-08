/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public interface Metadata {

  static Metadata parse(ByteInput input) throws IOException {
    final var blockId = BlockTypeId.parse(input);
    if (MetadataBlock.ID.equals(blockId)) {
      return MetadataBlock.parse(input);
    } else if (TextBlock.ID.equals(blockId)) {
      return TextBlock.parse(input);
    } else {
      throw new FormatException("Expected MD or TX block, bot got " + blockId);
    }
  }

  <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E;

  interface Visitor<R, E extends Throwable> {
    R visit(TextBlock value) throws E;

    R visit(MetadataBlock value) throws E;
  }

  Type TYPE = new Type();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  class Type implements BlockType<Metadata> {

    @Override
    public BlockTypeId id() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Metadata parse(ByteInput input) throws IOException {
      return Metadata.parse(input);
    }
  }
}
