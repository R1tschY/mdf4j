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
import java.io.UncheckedIOException;

@Value
public class DataGroup {

    Link<DataGroup> nextDataGroup; // DG
    Link<ChannelGroup> firstChannelGroup; // CG
    Link<DataRoot> data; // DT,DV,DZ,DL,LD,HL
    Link<TextBased> comment; // TX,MD

    int recordIdSize;

    public java.util.Iterator<ChannelGroup> iterChannelGroups(ByteInput input) {
        return new ChannelGroup.Iterator(firstChannelGroup, input);
    }

    public static DataGroup parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parseExpecting(BlockId.DG, input, 4, 1);
        final var recordIdSize = input.readU8();

        final var links = blockHeader.getLinks();
        return new DataGroup(Link.of(links[0]), Link.of(links[1]), Link.of(links[2]), Link.of(links[3]), recordIdSize);
    }

    public static class Iterator implements java.util.Iterator<DataGroup> {
        private final ByteInput input;
        private Link<DataGroup> next;

        public Iterator(Link<DataGroup> start, ByteInput input) {
            this.input = input;
            this.next = start;
        }

        @Override
        public boolean hasNext() {
            return !next.isNil();
        }

        @Override
        public DataGroup next() {
            try {
                final var dataGroup = next.resolve(DataGroup.META, input).orElseThrow();
                next = dataGroup.getNextDataGroup();
                return dataGroup;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static final Meta META = new Meta();
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Meta implements FromBytesInput<DataGroup> {
        @Override
        public DataGroup parse(ByteInput input) throws IOException {
            return DataGroup.parse(input);
        }
    }
}
