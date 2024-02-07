/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class SourceInformationBlock {

  Link<TextBlockBlock> sourceName;
  Link<TextBlockBlock> sourcePath;
  Link<TextBasedBlock> commentOrMetadata;
  byte type;
  byte busType;
  byte flags;

  public static SourceInformationBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parse(ID, input);
    final var type = input.readU8();
    final var busType = input.readU8();
    final var flags = input.readU8();
    final var links = blockHeader.getLinks();
    return new SourceInformationBlock(Link.of(links[0]), Link.of(links[1]), Link.of(links[2]), type,
        busType, flags);
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('S', 'I');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements BlockType<SourceInformationBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public SourceInformationBlock parse(ByteInput input) throws IOException {
      return SourceInformationBlock.parse(input);
    }
  }
}

