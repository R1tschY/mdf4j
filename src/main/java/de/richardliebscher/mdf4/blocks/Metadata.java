/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import static de.richardliebscher.mdf4.blocks.ParseUtils.parseText;

import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class Metadata implements TextBased {

  String xml;

  public static final Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Meta implements FromBytesInput<Metadata> {

    @Override
    public Metadata parse(ByteInput input) throws IOException {
      return Metadata.parse(input);
    }
  }

  public static Metadata parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parse(BlockType.MD, input);
    return new Metadata(parseText(input, blockHeader.getDataLength()));
  }

  @Override
  public <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {
    return visitor.visit(this);
  }
}
