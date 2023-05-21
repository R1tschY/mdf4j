/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.internal.FileContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Channel group.
 */
@RequiredArgsConstructor
public class ChannelGroup {

  private final ChannelGroupBlock block;
  private final FileContext ctx;

  /**
   * Get low-level block structure.
   *
   * @return Corresponding MDF4 block
   */
  public ChannelGroupBlock getBlock() {
    return block;
  }

  /**
   * Get channel group display name if existing.
   *
   * @return Channel group name
   * @throws IOException Failed to read name from file.
   */
  public Optional<String> getName() throws IOException {
    return ctx.readName(block.getComment(), "CGcomment");
  }

  /**
   * Create iterator over channels of this channel group.
   *
   * @return Newly created iterator
   */
  public java.util.Iterator<Channel> iterChannels() {
    return new Channel.Iterator(block.getFirstChannel(), ctx);
  }

  static class Iterator implements java.util.Iterator<ChannelGroup> {

    private final FileContext ctx;
    private Link<ChannelGroupBlock> next;

    Iterator(Link<ChannelGroupBlock> start, FileContext ctx) {
      this.ctx = ctx;
      this.next = start;
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public ChannelGroup next() {
      try {
        final var dataGroup = next.resolve(ChannelGroupBlock.META,
            ctx.getInput()).orElseThrow();
        next = dataGroup.getNextChannelGroup();
        return new ChannelGroup(dataGroup, ctx);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
