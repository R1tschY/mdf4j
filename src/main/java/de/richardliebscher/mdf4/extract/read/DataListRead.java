/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.BlockType;
import de.richardliebscher.mdf4.blocks.ChannelBlockData;
import de.richardliebscher.mdf4.blocks.ChannelBlockDataZipped;
import de.richardliebscher.mdf4.blocks.ChannelDataBlock;
import de.richardliebscher.mdf4.blocks.DataListBlock;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;

public class DataListRead implements DataRead {

  private final ByteInput input;
  private DataListBlock dataList;
  private long remainingDataLength;
  private ReadableByteChannel currentBlock;
  private boolean closed = false;
  private Iterator<Link<ChannelDataBlock>> dataBlocks;

  public DataListRead(ByteInput input, DataListBlock firstDataList) {
    this.input = input;
    this.dataList = firstDataList;
    this.dataBlocks = firstDataList.getData().iterator();
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (closed) {
      throw new ClosedChannelException();
    }

    final var hasData = ensureDataStream();
    if (!hasData) {
      return -1;
    }

    final int remaining = (int) Math.min(remainingDataLength, dst.remaining());
    final var bytes = currentBlock.read(dst.slice().limit(remaining));
    if (bytes > 0) {
      remainingDataLength -= bytes;
    }
    return bytes;
  }

  private boolean ensureDataStream() throws IOException {
    if (remainingDataLength == 0) {
      if (dataBlocks == null || !dataBlocks.hasNext()) {
        dataList = dataList.getNextDataList().resolve(DataListBlock.META, input).orElse(null);
        if (dataList == null) {
          return false;
        }
        dataBlocks = dataList.getData().iterator();
        if (!dataBlocks.hasNext()) {
          return ensureDataStream();
        }
      }

      final var dataBlock = dataBlocks.next().resolveNonCached(ChannelDataBlock.META, input)
          .orElseThrow(() -> new FormatException("Data link in DL block should not be NIL"));
      if (dataBlock instanceof ChannelBlockData) {
        currentBlock = dataBlock.getChannel(input);
        remainingDataLength = ((ChannelBlockData) dataBlock).getDataLength();
      } else if (dataBlock instanceof ChannelBlockDataZipped) {
        final var dataZipped = (ChannelBlockDataZipped) dataBlock;
        if (dataZipped.getOriginalBlockType().equals(BlockType.DT)) {
          currentBlock = dataZipped.getChannel(input);
          remainingDataLength = dataZipped.getOriginalDataLength();
        } else {
          throw new FormatException("Unexpected data block type in zipped data: "
              + dataZipped.getOriginalBlockType());
        }
      } else {
        throw new FormatException("Unexpected data block in data list: " + dataBlock);
      }
    }

    return true;
  }

  @Override
  public boolean isOpen() {
    return !closed;
  }

  @Override
  public void close() throws IOException {
    if (currentBlock != null) {
      currentBlock.close();
    }
    closed = true;
  }
}
