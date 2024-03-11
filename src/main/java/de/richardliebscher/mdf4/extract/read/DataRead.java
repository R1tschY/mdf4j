/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.blocks.BlockType;
import de.richardliebscher.mdf4.blocks.Data;
import de.richardliebscher.mdf4.blocks.DataContainer;
import de.richardliebscher.mdf4.blocks.DataListBlock;
import de.richardliebscher.mdf4.blocks.DataStorage;
import de.richardliebscher.mdf4.blocks.HeaderListBlock;
import de.richardliebscher.mdf4.blocks.ZipType;
import de.richardliebscher.mdf4.exceptions.NotImplementedFeatureException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

public interface DataRead<T extends Data<T>> extends SeekableByteChannel {

  @Override
  default int write(ByteBuffer src) throws IOException {
    throw new NonWritableChannelException();
  }

  @Override
  default SeekableByteChannel truncate(long size) throws IOException {
    throw new NonWritableChannelException();
  }

  @SuppressWarnings("unchecked")
  static <T extends Data<T>> DataRead<T> of(DataContainer<T> dataRoot, ByteInput input,
      BlockType<DataStorage<T>> storageBlockType)
      throws IOException {
    if (dataRoot == null) {
      return new EmptyDataRead<>();
    } else if (dataRoot instanceof DataStorage) {
      return new DataStorageRead<>(input, (DataStorage<T>) dataRoot);
    } else if (dataRoot instanceof DataListBlock) {
      return new DataListRead<>(input, (DataListBlock<T>) dataRoot, storageBlockType);
    } else if (dataRoot instanceof HeaderListBlock) {
      final var headerList = (HeaderListBlock<T>) dataRoot;

      if (headerList.getZipType() != ZipType.DEFLATE) {
        throw new NotImplementedFeatureException(
            "ZIP type not implemented: " + headerList.getZipType());
      }

      return headerList.getFirstDataList().resolve(DataListBlock.type(), input)
          .<DataRead<T>>map(
              firstDataList -> new DataListRead<>(input, firstDataList, storageBlockType))
          .orElseGet(EmptyDataRead::new);
    } else {
      throw new IllegalStateException("Should not happen!");
    }
  }
}
