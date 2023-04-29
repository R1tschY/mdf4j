/*
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static de.richardliebscher.mdf4.blocks.ParseUtils.flagsSet;

/**
 * Header/HD-Block.
 */
@Value
public class Header {
    // Time flags
    private static final byte OFFSET_VALID = 1 << 1;
    // Time quality class
    private static final byte TIME_SRC_PC = 0;

    Link<DataGroup> firstDataGroup;
    long firstFileHistory;
    long firstChannelHierarchy;
    long firstAttachment;
    long firstEventBlock;
    Link<TextBased> comment;

    /**
     * Absolute start time in nanoseconds since midnight Jan 1st, 1970
     */
    Instant startTime;
    /**
     * Time zone offset in minutes
     */
    ZoneOffset timeZoneOffset;
    /**
     * Daylight saving time (DST) offset in minutes
     */
    ZoneOffset dstOffset;

    byte timeClass;

    byte flags;

    float startAngleRad;

    float startDistanceM;

    public java.util.Iterator<DataGroup> iterDataGroups(ByteInput input) {
        return new DataGroup.Iterator(firstDataGroup, input);
    }
    public Optional<TextBased> readComment(ByteInput input) throws IOException {
        return comment.resolve(TextBased.META, input);
    }

    public static Header parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parseExpecting(BlockType.HD, input, 6, 24);
        final var startTime = ParseUtils.toInstant(input.readI64LE());
        final var tzOffsetMin = input.readI16LE();
        final var dstOffsetMin = input.readI16LE();
        final var timeFlags = input.readU8();
        final var timeClass = input.readU8();
        final var flags = input.readU8();
        input.skip(1);
        final var startAngleRad = input.readF32LE();
        final var startDistanceM = input.readF32LE();

        final var links = blockHeader.getLinks();
        return new Header(
                Link.of(links[0]),
                links[1],
                links[2],
                links[3],
                links[4],
                Link.of(links[5]),
                startTime,
                flagsSet(timeFlags, OFFSET_VALID) ? ZoneOffset.ofTotalSeconds(tzOffsetMin * 60) : null,
                flagsSet(timeFlags, OFFSET_VALID) ? ZoneOffset.ofTotalSeconds(dstOffsetMin * 60) : null,
                timeClass,
                flags,
                startAngleRad,
                startDistanceM
        );
    }

    public static final Meta META = new Meta();
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Meta implements FromBytesInput<Header> {
        @Override
        public Header parse(ByteInput input) throws IOException {
            return Header.parse(input);
        }
    }
}
