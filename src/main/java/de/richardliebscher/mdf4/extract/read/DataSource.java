/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.InternalReader;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataGroup;
import de.richardliebscher.mdf4.blocks.DataList;
import de.richardliebscher.mdf4.blocks.DataRoot;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;


@RequiredArgsConstructor
public class DataSource {
    public static DataRead create(InternalReader reader, DataGroup dataGroup) throws IOException {
        DataRead dataRead;
        final var dataRoot = dataGroup.getData().resolve(DataRoot.META, reader.getInput()).orElse(null);
        if (dataRoot == null) {
            dataRead = new EmptyDataRead();
        } else if (dataRoot instanceof Data) {
            dataRead = new ByteBufferRead(ByteBuffer.wrap(((Data) dataRoot).getData()));
        } else if (dataRoot instanceof DataList) {
            dataRead = new DataListRead(reader.getInput(), (DataList) dataRoot);
        } else {
            throw new IllegalStateException("Should not happen");
        }

        return dataRead;
    }
}
