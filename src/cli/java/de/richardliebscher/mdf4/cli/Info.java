package de.richardliebscher.mdf4.cli;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.Mdf4File;
import de.richardliebscher.mdf4.blocks.Block;
import de.richardliebscher.mdf4.blocks.BlockReader;
import de.richardliebscher.mdf4.blocks.UnknownBlock;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class Info {
    public static void main(String[] args) throws IOException {
        final var filePath = Path.of(args[0]);
        try (var reader = Mdf4File.open(filePath);
             var writer = new OutputStreamWriter(System.out)) {
            new BlockTreeWriter(reader.blockReader(), writer).writeTree(Link.nil(), reader.getHeader());
        }
    }

    private static class BlockTreeWriter {
        private final BlockReader reader;
        private final Writer writer;
        private final Set<Link<?>> seen = new HashSet<>();
        private boolean[] last;
        private int level = 0;

        private BlockTreeWriter(BlockReader reader, Writer writer) {
            this.reader = reader;
            this.writer = writer;
        }

        public void writeTree(Link<?> link, Block block) throws IOException {
            writer.write(block.typeId().toString());
            writer.write(" (0x");
            writer.write(Long.toHexString(link.asLong()));
            writer.write(")");

            if (seen.contains(link)) {
                writer.write("already seen");
                return;
            } else {
                seen.add(link);
                writer.write("\n");
            }

            var innerIdent = ident(level);
            for (Link<?> innerLink : block.links()) {
                writer.write(innerIdent);
                writer.write("\\- ");

                final var linkedBlock = reader.readBlock(innerLink.asUnknown(), UnknownBlock.TYPE);
                if (linkedBlock.isPresent()) {
                    level += 1;
                    writeTree(innerLink, linkedBlock.get());
                    level -= 1;
                } else {
                    writer.write("NIL\n");
                }
            }
        }

        private String ident(int level) {
            return "  ".repeat(level);
        }
    }
}
