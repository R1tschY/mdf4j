package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.IOException;

import static de.richardliebscher.mdf4.blocks.ParseUtils.parseText;

@Value
public class Metadata implements TextBased {
    String data;

    public static final Meta META = new Meta();
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Meta implements FromBytesInput<Metadata> {
        @Override
        public Metadata parse(ByteInput input) throws IOException {
            return Metadata.parse(input);
        }
    }

    public static Metadata parse(ByteInput input) throws IOException {
        final var blockHeader = BlockHeader.parse(BlockId.MD, input);
        return new Metadata(parseText(input, blockHeader.getDataLength()));
    }
}
