/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import static de.richardliebscher.mdf4.blocks.ParseUtils.parseText;

import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class TextBlockBlock implements TextBasedBlock {

  String text;

  public static TextBlockBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parse(ID, input);
    return new TextBlockBlock(parseText(input, blockHeader.getDataLength()));
  }

  @Override
  public <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {
    return visitor.visit(this);
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('T', 'X');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements BlockType<TextBlockBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public TextBlockBlock parse(ByteInput input) throws IOException {
      return TextBlockBlock.parse(input);
    }
  }
}
