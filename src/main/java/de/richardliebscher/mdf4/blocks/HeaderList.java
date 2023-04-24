/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.exceptions.ParseException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.IOException;

@Value
public class HeaderList implements DataRoot {

    Link<DataList> firstDataList;

    HeaderListFlags flags;
    ZipType zipType;

    public static HeaderList parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parseExpecting(BlockId.HL, input, 1, 3);
        final var links = blockHeader.getLinks();
        final Link<DataList> firstDataList = Link.of(links[0]);

        final var flags = HeaderListFlags.of(input.readI16LE());
        if (flags.hasUnknown()) {
            throw new ParseException("Future MDF4 could not be read: Unknown flags set in HL block");
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

