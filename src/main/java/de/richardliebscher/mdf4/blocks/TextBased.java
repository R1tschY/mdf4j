package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.Optional;

import static de.richardliebscher.mdf4.blocks.ParseUtils.peekBlockId;

public interface TextBased {

    static TextBased parse(ByteInput input) throws IOException {
        final var blockId = peekBlockId(input);
        if (BlockId.MD.equals(blockId)) {
            return Metadata.parse(input);
        } else if (BlockId.TX.equals(blockId)) {
            return Text.parse(input);
        } else {
            throw new FormatException("Expected MD or TX block, bot got " + blockId);
        }
    }

    Meta META = new Meta();
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class Meta implements FromBytesInput<TextBased> {
        @Override
        public TextBased parse(ByteInput input) throws IOException {
            return TextBased.parse(input);
        }
    }
}
