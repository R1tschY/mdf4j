/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.BlockType;
import de.richardliebscher.mdf4.blocks.WriteData;
import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.ReadWrite;
import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;
import lombok.EqualsAndHashCode;

/**
 * Typed link to a block.
 *
 * @param <T> Block type
 */
@EqualsAndHashCode
public final class Link<T> implements Serializable, WriteData {

  private static final Link<?> NIL = new Link<>(0);

  private final long link;
  private volatile T loaded;

  /**
   * Create link from long.
   *
   * @param link Link
   */
  private Link(long link) {
    this.link = link;
  }

  /**
   * Create NIL link.
   *
   * @param <T> Block type
   * @return NIL
   */
  @SuppressWarnings("unchecked")
  public static <T> Link<T> nil() {
    return (Link<T>) NIL;
  }

  /**
   * Create link from long.
   *
   * @param link Link
   */
  public static <T> Link<T> of(long link) {
    return link == 0 ? nil() : new Link<>(link);
  }

  /**
   * Get link as long.
   *
   * @return link as long
   */
  public long asLong() {
    return link;
  }

  /**
   * Check for NIL.
   *
   * @return {@code true} iff link is NIL
   */
  public boolean isNil() {
    return link == 0;
  }

  /**
   * Resolve link.
   *
   * <p>
   *   Caches block
   * </p>
   *
   * @param resolver Parser for block type
   * @param input    Input file
   * @return Block iff block is not NIL
   * @throws IOException Unable to read structure from file
   * @see #resolveNonCached
   */
  public Optional<T> resolve(BlockType<T> resolver, ByteInput input)
      throws IOException {
    if (link != 0) {
      var loadedLocal = loaded;
      if (loadedLocal == null) {
        synchronized (this) {
          loadedLocal = loaded;
          if (loadedLocal == null) {
            input.seek(link);
            loadedLocal = loaded = resolver.parse(input);
          }
        }
      }

      return Optional.of(loadedLocal);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Resolve link without cache.
   *
   * @param resolver Parser for block type
   * @param input    Input file
   * @return Block iff block is not NIL
   * @throws IOException Unable to read structure from file
   */
  public Optional<T> resolveNonCached(BlockType<T> resolver, ByteInput input)
      throws IOException {
    if (link != 0) {
      input.seek(link);
      return Optional.of(resolver.parse(input));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public String toString() {
    return "Link{0x" + Long.toHexString(link).toUpperCase(Locale.ROOT) + '}';
  }

  @Override
  public void write(ReadWrite input) throws IOException {
    input.write(link);
  }
}
