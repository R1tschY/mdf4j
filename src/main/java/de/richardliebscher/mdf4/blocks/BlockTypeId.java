/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class BlockTypeId {

  private final int id;

  private BlockTypeId(int id) {
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

  public static BlockTypeId of(byte a, byte b) {
    return BlockTypeId.of((char) a, (char) b);
  }

  static BlockTypeId of(char a, char b) {
    return new BlockTypeId('#' | ('#' << 8) | (a << 16) | (b << 24));
  }

  public static BlockTypeId parse(ByteInput input) throws IOException {
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
    return BlockTypeId.of(first, second);
  }

  @Override
  public String toString() {
    final var chars = new char[2];
    chars[0] = getFirstChar();
    chars[1] = getSecondChar();
    return new String(chars);
  }
}
