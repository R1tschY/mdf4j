/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.DataGroupBlock;
import de.richardliebscher.mdf4.internal.FileContext;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Data group.
 */
@RequiredArgsConstructor
public class DataGroup {

  private final DataGroupBlock block;
  private final FileContext ctx;

  /**
   * Get low-level block structure.
   *
   * @return Corresponding MDF4 block
   */
  public DataGroupBlock getBlock() {
    return block;
  }

  /**
   * Get data group display name if existing.
   *
   * @return Data group name
   * @throws IOException Failed to read name from file.
   */
  public Optional<String> getName() throws IOException {
    return ctx.readName(block.getComment(), "DGcomment");
  }

  /**
   * Create iterator for channel groups.
   *
   * @return Newly created iterator
   */
  public LazyIoList<ChannelGroup> getChannelGroups() {
    return () -> new ChannelGroup.Iterator(block.getFirstChannelGroup(), ctx);
  }

  static class Iterator implements LazyIoIterator<DataGroup> {

    private final FileContext ctx;
    private Link<DataGroupBlock> next;

    Iterator(Link<DataGroupBlock> start, FileContext ctx) {
      this.ctx = ctx;
      this.next = start;
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public DataGroup next() throws IOException {
      final var dataGroup = next.resolve(DataGroupBlock.META, ctx.getInput()).orElse(null);
      if (dataGroup == null) {
        return null;
      }
      next = dataGroup.getNextDataGroup();
      return new DataGroup(dataGroup, ctx);
    }
  }
}
