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
import java.nio.charset.StandardCharsets;

@Value
public class ChannelGroup {

    Link<ChannelGroup> nextChannelGroup;
    Link<Channel> firstChannel;
    Link<Text> acquisitionName;
    Link<SourceInformation> acquisitionSource;
    long firstSampleReduction; // SR
    Link<TextBased> comment;

    long recordId;
    long cycleCount;
    ChannelGroupFlags flags;
    char pathSeparator;
    int dataBytes;
    int invalidationBytes;

    public java.util.Iterator<Channel> iterChannels(ByteInput input) {
        return new Channel.Iterator(firstChannel, input);
    }

    public static ChannelGroup parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parse(BlockId.CG, input);
        final var links = blockHeader.getLinks();
        final Link<ChannelGroup> nextChannelGroup = Link.of(links[0]);
        final Link<Channel> firstChannel = Link.of(links[1]);
        final Link<Text> acquisitionName = Link.of(links[2]);
        final Link<SourceInformation> acquisitionSource = Link.of(links[3]);
        final var firstSampleReduction = links[4];
        final Link<TextBased> comment = Link.of(links[5]);

        final var recordId = input.readI64LE();
        final var cycleCount = input.readI64LE();
        final var flags = ChannelGroupFlags.of(input.readI16LE());
        final var pathSeparator = input.readString(2, StandardCharsets.UTF_16LE).charAt(0);
        input.skip(4);
        final var dataBytes = input.readI32LE();
        final var invalidationBits = input.readI32LE();

        return new ChannelGroup(
                nextChannelGroup, firstChannel, acquisitionName, acquisitionSource, firstSampleReduction, comment,
                recordId, cycleCount, flags, pathSeparator, dataBytes, invalidationBits);
    }

    public static class Iterator implements java.util.Iterator<ChannelGroup> {
        private final ByteInput input;
        private Link<ChannelGroup> next;

        public Iterator(Link<ChannelGroup> start, ByteInput input) {
            this.input = input;
            this.next = start;
        }

        @Override
        public boolean hasNext() {
            return !next.isNil();
        }

        @Override
        public ChannelGroup next() {
            try {
                final var channelGroup = next.resolve(ChannelGroup.META, input)
                        .orElseThrow();
                next = channelGroup.getNextChannelGroup();
                return channelGroup;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static final Meta META = new Meta();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Meta implements FromBytesInput<ChannelGroup> {
        @Override
        public ChannelGroup parse(ByteInput input) throws IOException {
            return ChannelGroup.parse(input);
        }
    }
}

