/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.exceptions.UnsupportedVersionException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class HeaderListBlock implements DataRootBlock {

  Link<DataListBlock> firstDataList;

  BitFlags<HeaderListFlag> flags;
  ZipType zipType;

  public static HeaderListBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(ID, input, 1, 3);
    final var links = blockHeader.getLinks();
    final Link<DataListBlock> firstDataList = Link.of(links[0]);

    final var flags = BitFlags.of(input.readI16(), HeaderListFlag.class);
    if (flags.hasUnknown()) {
      throw new UnsupportedVersionException(
          "Future MDF4 could not be read: Unknown flags set in HL block");
    }
    final var zipType = ZipType.parse(input.readU8());

    return new HeaderListBlock(firstDataList, flags, zipType);
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('H', 'L');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements BlockType<HeaderListBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public HeaderListBlock parse(ByteInput input) throws IOException {
      return HeaderListBlock.parse(input);
    }
  }
}

