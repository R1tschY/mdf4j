/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.Value;

@Value
public class BlockHeader {

  long length;
  long[] links;

  public long getDataLength() {
    return length - 24L - links.length * 8L;
  }

  public static BlockHeader parse(BlockType id, ByteInput input) throws IOException {
    final var typeId = input.readI32();
    if (id.asInt() != typeId) {
      throw newWrongBlockTypeException(id, typeId);
    }

    input.skip(4); // padding
    final var length = input.readI64();
    final var linkCount = input.readI64();

    final var links = new long[Math.toIntExact(linkCount)];
    for (int i = 0; i < linkCount; i++) {
      links[i] = input.readI64();
    }
    return new BlockHeader(length, links);
  }

  private static FormatException newWrongBlockTypeException(BlockType expected, int typeId) {
    final byte hash1 = (byte) (typeId & 0xFF);
    final byte hash2 = (byte) ((typeId >> 8) & 0xFF);
    if (hash1 != '#' || hash2 != '#') {
      return new FormatException(String.format(
          "Block type does not start with '##', got %02x%02x", hash1, hash2));
    }
    final byte first = (byte) ((typeId >> 16) & 0xFF);
    final byte second = (byte) ((typeId >> 24) & 0xFF);
    return new FormatException(String.format(
        "Expected block type '%s', got %s (%02x%02x)", expected,
        new String(new char[]{(char) first, (char) second}), first, second));
  }

  public static BlockHeader parseExpecting(BlockType id, ByteInput input, int links, int miniumSize)
      throws IOException {
    final var blockHeader = parse(id, input);
    if (blockHeader.links.length < links) {
      throw new FormatException(
          "Expecting a minium of " + links + " in " + id + " block, but got "
              + blockHeader.links.length + " links");
    }
    if (blockHeader.getDataLength() < miniumSize) {
      throw new FormatException(
          "Expecting a minium of " + miniumSize + " data bytes in " + id + " block, but got "
              + blockHeader.getDataLength() + " data bytes");
    }
    return blockHeader;
  }
}
