/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.exceptions.UnsupportedVersionException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class HeaderList implements DataRoot {

  Link<DataList> firstDataList;

  BitFlags<HeaderListFlag> flags;
  ZipType zipType;

  public static HeaderList parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(BlockType.HL, input, 1, 3);
    final var links = blockHeader.getLinks();
    final Link<DataList> firstDataList = Link.of(links[0]);

    final var flags = BitFlags.of(input.readI16(), HeaderListFlag.class);
    if (flags.hasUnknown()) {
      throw new UnsupportedVersionException(
          "Future MDF4 could not be read: Unknown flags set in HL block");
    }
    final var zipType = ZipType.parse(input.readU8());

    return new HeaderList(firstDataList, flags, zipType);
  }

  public static final Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Meta implements FromBytesInput<HeaderList> {

    @Override
    public HeaderList parse(ByteInput input) throws IOException {
      return HeaderList.parse(input);
    }
  }
}

