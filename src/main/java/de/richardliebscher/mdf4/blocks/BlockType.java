/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@AllArgsConstructor(staticName = "of")
public final class BlockType {
    public static final BlockType HD = BlockType.of('#', '#', 'H', 'D');
    public static final BlockType TX = BlockType.of('#', '#', 'T', 'X');
    public static final BlockType MD = BlockType.of('#', '#', 'M', 'D');
    public static final BlockType DG = BlockType.of('#', '#', 'D', 'G');
    public static final BlockType CG = BlockType.of('#', '#', 'C', 'G');
    public static final BlockType CN = BlockType.of('#', '#', 'C', 'N');
    public static final BlockType DT = BlockType.of('#', '#', 'D', 'T');
    public static final BlockType SI = BlockType.of('#', '#', 'S', 'I');
    public static final BlockType DL = BlockType.of('#', '#', 'D', 'L');
    public static final BlockType CC = BlockType.of('#', '#', 'C', 'C');
    public static final BlockType HL = BlockType.of('#', '#', 'H', 'L');
    public static final BlockType DZ = BlockType.of('#', '#', 'D', 'Z');

    int id;

    public int asInt() {
        return id;
    }

    public static BlockType of(char a, char b, char c, char d) {
        return new BlockType(a | (b << 8) | (c << 16) | (d << 24));
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
