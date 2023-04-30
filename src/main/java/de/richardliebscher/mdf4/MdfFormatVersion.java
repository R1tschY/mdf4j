/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import lombok.Value;

/**
 * Version of MDF4 file.
 */
@Value(staticConstructor = "of")
public class MdfFormatVersion {

  private static final Pattern VERSION_RE = Pattern.compile("^(\\d)\\.(\\d\\d) {4}$");

  int major;
  int minor;

  /**
   * Parse MDF4 version from file.
   *
   * @param input Input file
   * @return version
   * @throws IOException Unable to parse version
   */
  public static MdfFormatVersion parse(ByteInput input) throws IOException {
    final var formatId = input.readString(8, StandardCharsets.ISO_8859_1);
    final var matcher = VERSION_RE.matcher(formatId);
    if (matcher.find()) {
      return new MdfFormatVersion(Integer.parseInt(matcher.group(1)),
          Integer.parseInt(matcher.group(2)));
    } else {
      throw new FormatException("Unable to parse MDF format version, got " + formatId);
    }
  }

  /**
   * Get version as number.
   *
   * @return MDF version
   */
  public int asInt() {
    return major * 100 + minor;
  }
}
