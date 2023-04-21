package de.richardliebscher.mdf4.blocks;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@AllArgsConstructor(staticName = "of")
public class BlockId {
    public static final BlockId HD = BlockId.of('#', '#', 'H', 'D');
    public static final BlockId TX = BlockId.of('#', '#', 'T', 'X');
    public static final BlockId MD = BlockId.of('#', '#', 'M', 'D');
    public static final BlockId DG = BlockId.of('#', '#', 'D', 'G');
    public static final BlockId CG = BlockId.of('#', '#', 'C', 'G');
    public static final BlockId CN = BlockId.of('#', '#', 'C', 'N');
    public static final BlockId DT = BlockId.of('#', '#', 'D', 'T');
    public static final BlockId SI = BlockId.of('#', '#', 'S', 'I');
    public static final BlockId DL = BlockId.of('#', '#', 'D', 'L');
    public static final BlockId CC = BlockId.of('#', '#', 'C', 'C');

    int id;

    public int asInt() {
        return id;
    }

    public static BlockId of(char a, char b, char c, char d) {
        return new BlockId(a | (b << 8) | (c << 16) | (d << 24));
    }

    @Override
    public String toString() {
        return new StringBuilder(4)
                .append((char) (id & 0xFF))
                .append((char) ((id >> 8) & 0xFF))
                .append((char) ((id >> 16) & 0xFF))
                .append((char) ((id >> 24) & 0xFF))
                .toString();
    }
}
