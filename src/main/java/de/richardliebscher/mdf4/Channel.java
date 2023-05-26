/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.Text;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.internal.FileContext;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Channel.
 */
@RequiredArgsConstructor
public class Channel {

  private final de.richardliebscher.mdf4.blocks.Channel block;
  private final FileContext ctx;

  /**
   * Get low-level block structure.
   *
   * @return Corresponding MDF4 block
   */
  public de.richardliebscher.mdf4.blocks.Channel getBlock() {
    return block;
  }

  /**
   * Get channel name.
   *
   * @return Channel name
   * @throws IOException Failed to read name from file.
   */
  public String getName() throws IOException {
    return block.getChannelName().resolve(Text.META, ctx.getInput())
        .orElseThrow(() -> new FormatException("Channel name link is required"))
        .getData();
  }

  public Optional<String> getPhysicalUnit() throws IOException {
    // TODO: Consider unit of conversion?
    return ctx.readName(block.getPhysicalUnit(), "TODO");
  }

  static class Iterator implements LazyIoIterator<Channel> {

    private final FileContext ctx;
    private Link<de.richardliebscher.mdf4.blocks.Channel> next;

    Iterator(Link<de.richardliebscher.mdf4.blocks.Channel> start, FileContext ctx) {
      this.ctx = ctx;
      this.next = start;
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public Channel next() throws IOException {
      final var dataGroup = next
          .resolve(de.richardliebscher.mdf4.blocks.Channel.META, ctx.getInput())
          .orElse(null);
      if (dataGroup == null) {
        return null;
      }
      next = dataGroup.getNextChannel();
      return new Channel(dataGroup, ctx);
    }
  }
}
