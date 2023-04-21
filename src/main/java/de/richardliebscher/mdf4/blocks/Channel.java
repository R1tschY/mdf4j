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
public class Channel {

    Link<Channel> nextChannel;
    long component; // CA,CN
    Link<Text> channelName;
    long channelSource; // SI
    Link<ChannelConversion> conversionRule;
    long signalData; // cnType=1:SD,DZ,DL,HL,CG,cnType=4:AT,cnType=5:CN,event:EV
    Link<TextBased> physicalUnit;
    Link<TextBased> comment;
    // 4.1.0
    // long atReference; // AT
    // long defaultXDg;long defaultXCg;long defaultXCn; // DG+CG,CN

    ChannelType type;
    SyncType syncType;
    ChannelDataType dataType;
    byte bitOffset;
    int byteOffset;
    int bitCount;
    ChannelFlags flags;
    int invalidationBit;
    byte precision;
    short attachmentCount;
    double valueRangeMin;
    double valueRangeMax;
    double limitMin;
    double limitMax;
    double limitExtendedMin;
    double limitExtendedMax;

    public static Channel parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parseExpecting(BlockId.CN, input, 8, 72);
        final var type = ChannelType.parse(input.readU8());
        final var syncType = SyncType.parse(input.readU8());
        final var dataType = ChannelDataType.parse(input.readU8());
        final var bitOffset = input.readU8();
        final var byteOffset = input.readI32LE();
        final var bitCount = input.readI32LE();
        final var flags = ChannelFlags.of(input.readI32LE());
        final var invalidationBit = input.readI32LE();
        final var precision = input.readU8();
        input.skip(1);
        final var attachmentCount = input.readI16LE();
        final var valueRangeMin = input.readF64LE();
        final var valueRangeMax = input.readF64LE();
        final var limitMin = input.readF64LE();
        final var limitMax = input.readF64LE();
        final var limitExtendedMin = input.readF64LE();
        final var limitExtendedMax = input.readF64LE();

        final var links = blockHeader.getLinks();
        return new Channel(
                Link.of(links[0]), links[1], Link.of(links[2]), links[3], Link.of(links[4]), links[5], Link.of(links[6]), Link.of(links[7]),
                type, syncType, dataType, bitOffset, byteOffset, bitCount, flags,
                invalidationBit, precision, attachmentCount, valueRangeMin, valueRangeMax, limitMin, limitMax,
                limitExtendedMin, limitExtendedMax);
    }

    public static class Iterator implements java.util.Iterator<Channel> {
        private final ByteInput input;
        private Link<Channel> next;

        public Iterator(Link<Channel> start, ByteInput input) {
            this.input = input;
            this.next = start;
        }

        @Override
        public boolean hasNext() {
            return !next.isNil();
        }

        @Override
        public Channel next() {
            try {
                final var channel = next.resolve(Channel.META, input)
                        .orElseThrow();
                next = channel.getNextChannel();
                return channel;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static final Meta META = new Meta();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Meta implements FromBytesInput<Channel> {
        @Override
        public Channel parse(ByteInput input) throws IOException {
            return Channel.parse(input);
        }
    }
}

