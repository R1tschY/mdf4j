/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.ChannelGroupBlock;
import de.richardliebscher.mdf4.blocks.ChannelGroupFlag;
import de.richardliebscher.mdf4.internal.FileContext;
import java.io.IOException;
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
    return ctx.readText(block.getComment(), "CGcomment");
  }

  /**
   * Get information whether channel group contains bus events.
   *
   * @return {@code true} iff "Bus event channel group flag" is set.
   */
  public boolean containsBusEvents() {
    return block.getFlags().isSet(ChannelGroupFlag.BUS_EVENT_CHANNEL_GROUP);
  }

  /**
   * Get information whether channel group contains plain bus events.
   *
   * @return {@code true} iff "Plain bus event channel group flag" is set.
   */
  public boolean containsPlainBusEvents() {
    return block.getFlags().isSet(ChannelGroupFlag.PLAIN_BUS_EVENT_CHANNEL_GROUP);
  }

  /**
   * Create iterator over channels of this channel group.
   *
   * @return Newly created iterator
   */
  public LazyIoList<Channel> getChannels() {
    return () -> new Channel.Iterator(block.getFirstChannel(), ctx);
  }

  static class Iterator implements LazyIoIterator<ChannelGroup> {

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
    public ChannelGroup next() throws IOException {
      final var dataGroup = next.resolve(ChannelGroupBlock.TYPE, ctx.getInput()).orElse(null);
      if (dataGroup == null) {
        return null;
      }
      next = dataGroup.getNextChannelGroup();
      return new ChannelGroup(dataGroup, ctx);
    }
  }
}
