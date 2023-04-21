package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.IOException;

@Value
public class ChannelConversion {

    Link<Text> name;
    Link<TextBased> unit;
    Link<TextBased> comment;
    Link<ChannelConversion> inverse;
    // TODO: List<Link<Text | ChannelConversion>> ref

    ChannelConversionType type;
    int precision;
    ChannelConversionFlags flags;
    double physicalRangeMin;
    double physicalRangeMax;
    long[] vals;

    public static ChannelConversion parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parseExpecting(BlockId.CC, input, 4, 24);
        final var type = ChannelConversionType.parse(input.readU8());
        final var precision = Byte.toUnsignedInt(input.readU8());
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

        final var links = blockHeader.getLinks();
        return new ChannelConversion(
                Link.of(links[0]),
                Link.of(links[1]),
                Link.of(links[2]),
                Link.of(links[3]),
                type, precision, flags, physicalRangeMin, physicalRangeMax, vals);
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

