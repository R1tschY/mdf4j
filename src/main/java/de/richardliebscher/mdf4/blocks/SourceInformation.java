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

@Value
public class SourceInformation {

    Link<Text> sourceName;
    Link<Text> sourcePath;
    Link<TextBased> commentOrMetadata;
    byte type;
    byte busType;
    byte flags;

    public static SourceInformation parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parse(BlockType.SI, input);
        final var type = input.readU8();
        final var busType = input.readU8();
        final var flags = input.readU8();
        final var links = blockHeader.getLinks();
        return new SourceInformation(Link.of(links[0]), Link.of(links[1]), Link.of(links[2]), type, busType, flags);
    }

    public static final Meta META = new Meta();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Meta implements FromBytesInput<SourceInformation> {
        @Override
        public SourceInformation parse(ByteInput input) throws IOException {
            return SourceInformation.parse(input);
        }
    }
}

