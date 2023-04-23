/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

@RequiredArgsConstructor
public class ByteBufferRead implements DataRead {
    private final ByteBuffer data; // TODO: make thread safe
    private boolean closed = false;

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (closed) {
            throw new ClosedChannelException();
        }

        if (data.remaining() == 0) {
            return -1;
        }

        int bytesToRead = Math.min(data.remaining(), dst.remaining());
        dst.put(data.slice().limit(bytesToRead));
        return bytesToRead;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}
