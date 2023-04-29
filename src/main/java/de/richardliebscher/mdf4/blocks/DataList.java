/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.io.IOException;
import java.util.List;

import static de.richardliebscher.mdf4.internal.Arrays.newArray;

@Value
public class DataList implements DataRoot {

    Link<DataList> nextDataList;
    @ToString.Exclude
    List<Link<DataBlock>> data; // DT,SD,RD,DZ

    DataListFlags flags;
    long count;

    public static DataList parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parseExpecting(BlockType.DL, input, 1, 8);
        final var links = blockHeader.getLinks();
        final Link<DataList> nextDataList = Link.of(links[0]);
        final var data = getDataLinks(links);

        final var flags = DataListFlags.of(input.readU8());
        input.skip(3);
        final var count = input.readI32LE();
        if (count != data.size()) {
            throw new FormatException("Count attribute in DL block is inconsistent: " + count + " vs. " + data.size());
        }

//        if (flags.test(DataListFlags.EQUAL_LENGTH)) {
//            final var equalLength = input.readI64LE();
//            for (int i= 0; i < count; i++) {
//                final var offset = input.readQword(); // if equalLengthFlag
//            }
//        }
//
//        if (flags.test(DataListFlags.TIME_VALUES)) {
//            for (int i= 0; i < count; i++) {
//                final var time_values = input.readQword();
//            }
//        }
//
//        if (flags.test(DataListFlags.ANGLE_VALUES)) {
//            for (int i= 0; i < count; i++) {
//                final var angle_values = input.readQword();
//            }
//        }
//
//        if (flags.test(DataListFlags.DISTANCE_VALUES)) {
//            for (int i= 0; i < count; i++) {
//                final var distance_values = input.readQword();
//            }
//        }

        return new DataList(nextDataList, data, flags, count);
    }

    @SuppressWarnings("unchecked")
    private static List<Link<DataBlock>> getDataLinks(long[] links) {
        final var data = (Link<DataBlock>[]) newArray(Link.class, links.length - 1);
        for (int i = 1; i < links.length; i++) {
            data[i - 1] = Link.of(links[i]);
        }
        return List.of(data);
    }

    public static final Meta META = new Meta();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Meta implements FromBytesInput<DataList> {
        @Override
        public DataList parse(ByteInput input) throws IOException {
            return DataList.parse(input);
        }
    }
}

