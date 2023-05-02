/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import static de.richardliebscher.mdf4.blocks.Consts.FILE_MAGIC;
import static de.richardliebscher.mdf4.blocks.Consts.UNFINISHED_FILE_MAGIC;

import de.richardliebscher.mdf4.MdfFormatVersion;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.Value;

@Value
public class Id {

  MdfFormatVersion formatId;
  String programId;
  UnfinalizedFlags unfinalizedFlags;
  CustomFlags customUnfinalizedFlags;

  public boolean isUnfinalized() {
    return unfinalizedFlags != null;
  }

  public static Id parse(ByteInput input) throws IOException {
    final var fileId = input.readString(8, StandardCharsets.ISO_8859_1);
    final var version = MdfFormatVersion.parse(input);
    var program = input.readString(8, StandardCharsets.ISO_8859_1);
    final var programSize = program.indexOf('\0');
    if (programSize != -1) {
      program = program.substring(0, programSize);
    }

    final var defaultByteOrder = input.readI16Le(); // for 3.x
    final var defaultFloatingPointFormat = input.readI16Le(); // for 3.x
    final var versionNumber = input.readI16Le();
    final var codePageNumber = input.readI16Le(); // for 3.x
    input.skip(28); // fill bytes
    final UnfinalizedFlags unfinalizedFlags;
    final CustomFlags customUnfinalizedFlags;
    if (fileId.equals(UNFINISHED_FILE_MAGIC)) {
      unfinalizedFlags = UnfinalizedFlags.of(input.readI16Le());
      customUnfinalizedFlags = CustomFlags.of(input.readI16Le());
    } else {
      unfinalizedFlags = null;
      customUnfinalizedFlags = null;
    }

    if (!fileId.equals(FILE_MAGIC) && !fileId.equals(UNFINISHED_FILE_MAGIC)) {
      throw new FormatException(
          "File not a MDF file: file does not start with '" + FILE_MAGIC + "'");
    }

    if (version.asInt() != versionNumber) {
      throw new FormatException(
          "File MDF versions do not match " + version.asInt() + " vs " + versionNumber);
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
