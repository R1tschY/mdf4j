/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.MdfFormatVersion;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import lombok.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static de.richardliebscher.mdf4.blocks.Consts.FILE_MAGIC;

@Value
public class Id {
    MdfFormatVersion formatId;
    String programId;
    short unfinalizedFlags;
    short customUnfinalizedFlags;

    public static Id parse(ByteInput input) throws IOException {
        final var fileId = input.readString(8, StandardCharsets.ISO_8859_1);
        final var version = MdfFormatVersion.parse(input);
        final var program = input.readString(8, StandardCharsets.ISO_8859_1);
        final var defaultByteOrder = input.readI16LE();// for 3.x
        final var defaultFloatingPointFormat = input.readI16LE();// for 3.x
        final var versionNumber = input.readI16LE();
        final var codePageNumber = input.readI16LE();// for 3.x
        input.skip(28); // fill bytes
        final var unfinalizedFlags = input.readI16LE();
        final var customUnfinalizedFlags = input.readI16LE();

        if (!fileId.equals(FILE_MAGIC)) {
            throw new FormatException("File not a MDF file: file does not start with '" + FILE_MAGIC + "'");
        }

        if (version.asInt() != versionNumber) {
            throw new FormatException("File MDF versions do not match " + version.asInt() + " vs " + versionNumber);
        }

        if (defaultByteOrder != 0) {
            throw new FormatException("Unexpected non-default byte order");
        }

        if (defaultFloatingPointFormat != 0) {
            throw new FormatException("Unexpected non-default floating point format");
        }

        if (codePageNumber != 0) {
            throw new FormatException("Unexpected code page number");
        }

        return new Id(version, program, unfinalizedFlags, customUnfinalizedFlags);
    }

    public static class Meta implements FromBytesInput<Id> {
        @Override
        public Id parse(ByteInput input) throws IOException {
            return Id.parse(input);
        }
    }
}
