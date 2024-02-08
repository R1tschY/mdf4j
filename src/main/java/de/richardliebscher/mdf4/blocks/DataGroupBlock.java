/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.LazyIoIterator;
import de.richardliebscher.mdf4.LazyIoList;
import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class DataGroupBlock {

  Link<DataGroupBlock> nextDataGroup; // DG
  Link<ChannelGroupBlock> firstChannelGroup; // CG
  Link<DataContainer<DataBlock>> data; // DT,DV,DZ,DL,LD,HL
  Link<TextBasedBlock> comment; // TX,MD

  int recordIdSize;

  public LazyIoList<ChannelGroupBlock> getChannelGroups(ByteInput input) {
    return () -> new ChannelGroupBlock.Iterator(firstChannelGroup, input);
  }

  public static DataGroupBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parseExpecting(ID, input, 4, 1);
    final var recordIdSize = input.readU8();

    final var links = blockHeader.getLinks();
    return new DataGroupBlock(
        Link.of(links[0]), Link.of(links[1]), Link.of(links[2]), Link.of(links[3]),
        recordIdSize);
  }

  public static class Iterator implements LazyIoIterator<DataGroupBlock> {

    private final ByteInput input;
    private Link<DataGroupBlock> next;

    public Iterator(Link<DataGroupBlock> start, ByteInput input) {
      this.input = input;
      this.next = start;
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public DataGroupBlock next() throws IOException {
      final var dataGroup = next.resolve(TYPE, input).orElseThrow();
      next = dataGroup.getNextDataGroup();
      return dataGroup;
    }
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('D', 'G');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements BlockType<DataGroupBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public DataGroupBlock parse(ByteInput input) throws IOException {
      return DataGroupBlock.parse(input);
    }
  }
}
