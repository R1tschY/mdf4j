/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import static de.richardliebscher.mdf4.blocks.ChannelConversionFlag.PHYSICAL_VALUE_RANGE_VALID;
import static de.richardliebscher.mdf4.blocks.ChannelConversionFlag.PRECISION_VALID;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class ChannelConversion {

  public static final String UNIT_ELEMENT = "CCunit";

  Link<TextBlockBlock> name;
  Link<TextBasedBlock> unit;
  Link<TextBasedBlock> comment;
  Link<ChannelConversion> inverse;
  // TODO: List<Link<TextBlockBlock | ChannelConversion>> ref

  ChannelConversionType type;
  Integer precision;
  BitFlags<ChannelConversionFlag> flags;
  Range physicalRange;
  long[] vals;

  public static ChannelConversion parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(BlockType.CC, input, 4, 24);
    final var type = ChannelConversionType.parse(input.readU8());
    final var maybePrecision = Byte.toUnsignedInt(input.readU8());
    final var flags = BitFlags.of(input.readI16(), ChannelConversionFlag.class);
    /* final var refCount = Short.toUnsignedInt(*/
    input.readI16();
    final var valCount = Short.toUnsignedInt(input.readI16());
    final var physicalRangeMin = input.readF64();
    final var physicalRangeMax = input.readF64();

    final var vals = new long[valCount];
    for (int i = 0; i < valCount; i++) {
      vals[i] = input.readI64();
    }

    final var precision = flags.isSet(PRECISION_VALID) ? maybePrecision : null;
    final var physicalRange = flags.isSet(PHYSICAL_VALUE_RANGE_VALID)
        ? new Range(physicalRangeMin, physicalRangeMax)
        : null;

    final var links = blockHeader.getLinks();
    return new ChannelConversion(
        Link.of(links[0]),
        Link.of(links[1]),
        Link.of(links[2]),
        Link.of(links[3]),
        type, precision, flags, physicalRange, vals);
  }

  public static final Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Meta implements FromBytesInput<ChannelConversion> {

    @Override
    public ChannelConversion parse(ByteInput input) throws IOException {
      return ChannelConversion.parse(input);
    }
  }
}

