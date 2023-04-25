/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.IOException;

import static de.richardliebscher.mdf4.blocks.ChannelConversionFlags.PHYSICAL_VALUE_RANGE_VALID;
import static de.richardliebscher.mdf4.blocks.ChannelConversionFlags.PRECISION_VALID;

@Value
public class ChannelConversion {

    Link<Text> name;
    Link<TextBased> unit;
    Link<TextBased> comment;
    Link<ChannelConversion> inverse;
    // TODO: List<Link<Text | ChannelConversion>> ref

    ChannelConversionType type;
    Integer precision;
    ChannelConversionFlags flags;
    Range physicalRange;
    long[] vals;

    public static ChannelConversion parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parseExpecting(BlockId.CC, input, 4, 24);
        final var type = ChannelConversionType.parse(input.readU8());
        final var maybePrecision = Byte.toUnsignedInt(input.readU8());
        final var flags = ChannelConversionFlags.of(input.readI16LE());
        /* final var refCount = Short.toUnsignedInt(*/
        input.readI16LE();
        final var valCount = Short.toUnsignedInt(input.readI16LE());
        final var physicalRangeMin = input.readF64LE();
        final var physicalRangeMax = input.readF64LE();

        final var vals = new long[valCount];
        for (int i = 0; i < valCount; i++) {
            vals[i] = input.readI64LE();
        }

        final var precision = flags.test(PRECISION_VALID) ? maybePrecision : null;
        final var physicalRange = flags.test(PHYSICAL_VALUE_RANGE_VALID)
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

