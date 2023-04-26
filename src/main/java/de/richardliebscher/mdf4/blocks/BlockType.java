/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import lombok.EqualsAndHashCode;

import java.io.IOException;

@EqualsAndHashCode
public final class BlockType {
    public static final BlockType HD = BlockType.of('H', 'D');
    public static final BlockType TX = BlockType.of('T', 'X');
    public static final BlockType MD = BlockType.of('M', 'D');
    public static final BlockType DG = BlockType.of('D', 'G');
    public static final BlockType CG = BlockType.of('C', 'G');
    public static final BlockType CN = BlockType.of('C', 'N');
    public static final BlockType DT = BlockType.of('D', 'T');
    public static final BlockType SI = BlockType.of('S', 'I');
    public static final BlockType DL = BlockType.of('D', 'L');
    public static final BlockType CC = BlockType.of('C', 'C');
    public static final BlockType HL = BlockType.of('H', 'L');
    public static final BlockType DZ = BlockType.of('D', 'Z');

    private final int id;

    private BlockType(int id) {
        this.id = id;
    }

    public int asInt() {
        return id;
    }

    public char getFirstChar() {
        return (char) ((id >> 16) & 0xFF);
    }

    public char getSecondChar() {
        return (char) ((id >> 24) & 0xFF);
    }

    public static BlockType of(byte a, byte b) {
        return BlockType.of((char) a, (char) b);
    }

    private static BlockType of(char a, char b) {
        return new BlockType('#' | ('#' << 8) | (a << 16) | (b << 24));
    }

    public static BlockType parse(ByteInput input) throws IOException {
        final var backup = input.pos();
        final var hash1 = input.readU8();
        final var hash2 = input.readU8();
        final var first = input.readU8();
        final var second = input.readU8();
        input.seek(backup);
        if (hash1 != '#' || hash2 != '#') {
            throw new FormatException(
                    "Not a block: prefix: " + hash1 + "," + hash2);
        }
        return BlockType.of(first, second);
    }

    @Override
    public String toString() {
        final var chars = new char[2];
        chars[0] = getFirstChar();
        chars[1] = getSecondChar();
        return new String(chars);
    }
}