package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;

public interface DataRoot {

    static DataRoot parse(ByteInput input) throws IOException {
        final var blockId = ParseUtils.peekBlockId(input);
        if (blockId.equals(BlockId.DL)) {
            return DataList.parse(input);
        } else if (blockId.equals(BlockId.DT)) {
            return Data.parse(input);
        } else {
            throw new NotImplementedFeatureException("Root data block not implemented: " + blockId);
        }
    }

    Meta META = new Meta();
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class Meta implements FromBytesInput<DataRoot> {
        @Override
        public DataRoot parse(ByteInput input) throws IOException {
            return DataRoot.parse(input);
        }
    }
}
