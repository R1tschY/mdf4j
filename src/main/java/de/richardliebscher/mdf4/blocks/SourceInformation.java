package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.IOException;

@Value
public class SourceInformation {

    long sourceName; // TX
    long sourcePath; // TX
    long commentOrMetadata; // MD
    byte type;
    byte busType;
    byte flags;

    public static SourceInformation parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parse(BlockId.SI, input);
        final var type = input.readU8();
        final var busType = input.readU8();
        final var flags = input.readU8();
        final var links = blockHeader.getLinks();
        return new SourceInformation(links[0], links[1], links[2], type, busType, flags);
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

