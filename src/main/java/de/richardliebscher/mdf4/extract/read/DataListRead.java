/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.Link;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataBlock;
import de.richardliebscher.mdf4.blocks.DataList;
import de.richardliebscher.mdf4.blocks.DataZipped;
import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Iterator;

public class DataListRead implements DataRead {
    private final ByteInput input;
    private DataList dataList;
    private ByteBuffer currentBlock;
    private boolean closed = false;
    private Iterator<Link<DataBlock>> dataBlocks;

    public DataListRead(ByteInput input, DataList firstDataList) {
        this.input = input;
        this.dataList = firstDataList;
        this.dataBlocks = firstDataList.getData().iterator();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (closed) {
            throw new ClosedChannelException();
        }

        if (currentBlock == null || currentBlock.remaining() == 0) {
            if (dataBlocks == null || !dataBlocks.hasNext()) {
                dataList = dataList.getNextDataList().resolve(DataList.META, input).orElse(null);
                if (dataList == null) {
                    return -1;
                }
                dataBlocks = dataList.getData().iterator();
                if (!dataBlocks.hasNext()) {
                    return read(dst);
                }
            }

            final var dataBlock = dataBlocks.next().resolveNonCached(DataBlock.META, input)
                    .orElseThrow(() -> new FormatException("Data link in DL block should not be NIL"));
            if (dataBlock instanceof Data) {
                currentBlock = ByteBuffer.wrap(((Data) dataBlock).getData());
            } else if (dataBlock instanceof DataZipped) {
                final var uncompressedDataBlock = ((DataZipped) dataBlock).getUncompressed();
                if (uncompressedDataBlock instanceof Data) {
                    currentBlock = ByteBuffer.wrap(((Data) uncompressedDataBlock).getData());
                } else {
                    throw new FormatException("Unexpected data block in zipped data: " + uncompressedDataBlock);
                }
            } else {
                throw new FormatException("Unexpected data block in data list: " + dataBlock);
            }
        }

        int bytesToRead = Math.min(currentBlock.remaining(), dst.remaining());
        dst.put(currentBlock.slice().limit(bytesToRead));
        currentBlock.position(currentBlock.position() + bytesToRead);
        return bytesToRead;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}
