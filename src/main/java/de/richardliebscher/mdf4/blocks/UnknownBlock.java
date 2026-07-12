package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.extract.read.Links;
import de.richardliebscher.mdf4.internal.Pair;
import de.richardliebscher.mdf4.io.ByteInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class UnknownBlock implements Block {
    private final int typeId;
    private final Links<?> links;
    private final byte[] content;

    public UnknownBlock(int typeId, long[] links, byte[] content) {
        this.typeId = typeId;
        this.links = new Links<>(links);
        this.content = content;
    }

    @Override
    public BlockTypeId typeId() {
        return new BlockTypeId(typeId);
    }

    @Override
    public List<? extends Link<?>> links() {
        return links;
    }

    @Override
    public List<Map.Entry<String, String>> content() {
        return List.of(
                Pair.of("<base64>", new String(
                        Base64.getEncoder().encode(ByteBuffer.wrap(content, 0, Math.min(content.length, 128))).array(),
                        StandardCharsets.US_ASCII))
        );
    }

    @Override
    public Link<Metadata> metadataLink() {
        return null;
    }

    public static final UnknownBlock.Type TYPE = new UnknownBlock.Type();

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Type implements BlockType<UnknownBlock> {

        @Override
        public BlockTypeId id() {
            throw new UnsupportedOperationException();
        }

        @Override
        public UnknownBlock parse(ByteInput input) throws IOException {
            final var blockHeader = BlockHeader.parse(input);
            final var content = input.readBytes(Math.toIntExact(blockHeader.getDataLength()));
            return new UnknownBlock(blockHeader.getBlockTypeId(), blockHeader.getLinks(), content);
        }
    }
}
