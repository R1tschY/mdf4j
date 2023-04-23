/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.Header;
import de.richardliebscher.mdf4.blocks.Id;
import de.richardliebscher.mdf4.exceptions.ChannelGroupNotFoundException;
import de.richardliebscher.mdf4.extract.ChannelSelector;
import de.richardliebscher.mdf4.extract.RecordReader;
import de.richardliebscher.mdf4.extract.de.RecordVisitor;
import de.richardliebscher.mdf4.io.ByteInput;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.io.IOException;

import static de.richardliebscher.mdf4.blocks.Consts.HD_BLOCK_OFFSET;

@Log
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Mdf4File {
    private final InternalReader inner;

    public Header getHeader() {
        return inner.getHeader();
    }

    public Id getId() {
        return inner.getId();
    }

    public static Mdf4File open(ByteInput input) throws IOException {
        final var idBlock = Id.parse(input);
        input.seek(HD_BLOCK_OFFSET);
        final var hdBlock = Header.parse(input);

        log.info("Opened MDF4: Version=" + idBlock.getFormatId() + " StartTime=" + hdBlock.getStartTime());
        return new Mdf4File(new InternalReader(input, idBlock, hdBlock));
    }

    public <R> RecordReader<R> newRowReader(ChannelSelector selector, RecordVisitor<R> recordVisitor) throws ChannelGroupNotFoundException, IOException {
        return RecordReader.createFor(inner, selector, recordVisitor);
    }
}
