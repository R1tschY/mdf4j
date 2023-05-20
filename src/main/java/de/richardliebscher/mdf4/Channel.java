/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.Text;
import de.richardliebscher.mdf4.exceptions.FormatException;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;

/**
 * Channel.
 */
@RequiredArgsConstructor
public class Channel {

  private final de.richardliebscher.mdf4.blocks.Channel block;
  private final FileContext ctx;

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

  static class Iterator implements java.util.Iterator<Channel> {

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
    public Channel next() {
      try {
        final var dataGroup = next.resolve(de.richardliebscher.mdf4.blocks.Channel.META,
            ctx.getInput()).orElseThrow();
        next = dataGroup.getNextChannel();
        return new Channel(dataGroup, ctx);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
