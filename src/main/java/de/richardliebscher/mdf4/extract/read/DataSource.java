/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataGroupBlock;
import de.richardliebscher.mdf4.blocks.DataList;
import de.richardliebscher.mdf4.blocks.DataRoot;
import de.richardliebscher.mdf4.blocks.HeaderList;
import de.richardliebscher.mdf4.blocks.ZipType;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.internal.InternalReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class DataSource {

  public static DataRead create(InternalReader reader, DataGroupBlock dataGroup)
      throws IOException {
    final var input = reader.getInput();

    final var dataRoot = dataGroup.getData().resolve(DataRoot.META, input).orElse(null);
    if (dataRoot == null) {
      return new EmptyDataRead();
    } else if (dataRoot instanceof Data) {
      return new ByteBufferRead(ByteBuffer.wrap(((Data) dataRoot).getData()));
    } else if (dataRoot instanceof DataList) {
      return new DataListRead(input, (DataList) dataRoot);
    } else if (dataRoot instanceof HeaderList) {
      final var headerList = (HeaderList) dataRoot;

      if (headerList.getZipType() != ZipType.DEFLATE) {
        throw new NotImplementedFeatureException(
            "ZIP type not implemented: " + headerList.getZipType());
      }

      return headerList.getFirstDataList().resolve(DataList.META, input)
          .map(firstDataList -> (DataRead) new DataListRead(input, firstDataList))
          .orElseGet(EmptyDataRead::new);
    } else {
      throw new IllegalStateException("Should not happen");
    }
  }
}
