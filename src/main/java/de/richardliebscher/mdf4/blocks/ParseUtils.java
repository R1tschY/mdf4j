/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

class ParseUtils {
    static Instant toInstant(long nanoSeconds) {
        return Instant.ofEpochSecond(0, nanoSeconds);
    }

    static String parseText(ByteInput input, long length) throws IOException {
        final var data = input.readString(Math.toIntExact(length), StandardCharsets.UTF_8);
        final var size = data.indexOf('\0');
        if (size == -1) {
            throw new FormatException("Missing zero termination of text block");
        }
        return data.substring(0, size);
    }

    static BlockId peekBlockId(ByteInput input) throws IOException {
        final var backup = input.pos();
        final var id = input.readI32LE();
        input.seek(backup);
        return BlockId.of(id);
    }

    static boolean flagsSet(int toTest, int flags) {
        return (toTest & flags) == flags;
    }
}
