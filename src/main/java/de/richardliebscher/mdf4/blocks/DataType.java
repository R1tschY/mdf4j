/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.exceptions.FormatException;
import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

public interface DataType<T extends Data<T>> extends DataContainerType<T, T> {

  default DataContainer<T> parseDataContainer(ByteInput input) throws IOException {
    final var blockId = BlockTypeId.parse(input);
    if (blockId.equals(DataListBlock.ID)) {
      return DataListBlock.parse(input);
    } else if (blockId.equals(DataZippedBlock.ID)) {
      final var dataZipped = DataZippedBlock.<T>parse(input);
      if (!dataZipped.getOriginalBlockTypeId().equals(id())) {
        throw new FormatException(
            "Expected block type " + id() + " in zipped data block "
                + ", but got " + dataZipped.getOriginalBlockTypeId());
      }
      return dataZipped;
    } else if (blockId.equals(HeaderListBlock.ID)) {
      return HeaderListBlock.parse(input);
    } else if (blockId.equals(id())) {
      return parse(input);
    } else {
      throw new FormatException(
          "Expected block type " + id() + ", HL, DT or DZ, but got " + blockId);
    }
  }

  default DataStorage<T> parseStorage(ByteInput input) throws IOException {
    final var blockId = BlockTypeId.parse(input);
    if (blockId.equals(id())) {
      return parse(input);
    } else if (blockId.equals(DataZippedBlock.ID)) {
      final var dataZipped = DataZippedBlock.<T>parse(input);
      if (!dataZipped.getOriginalBlockTypeId().equals(id())) {
        throw new FormatException(
            "Expected block type " + id() + " in zipped data block "
                + ", but got " + dataZipped.getOriginalBlockTypeId());
      }
      return dataZipped;
    } else {
      throw new FormatException("Expected block type " + id() + " or DZ, but got " + blockId);
    }
  }

  @RequiredArgsConstructor
  class ContainerType<T extends Data<T>> implements
      DataContainerType<T, DataContainer<T>> {

    private final DataType<T> dataType;

    @Override
    public BlockTypeId id() {
      throw new UnsupportedOperationException();
    }

    @Override
    public DataContainer<T> parse(ByteInput input) throws IOException {
      return dataType.parseDataContainer(input);
    }
  }

  @RequiredArgsConstructor
  class StorageType<T extends Data<T>> implements BlockType<DataStorage<T>> {

    private final DataType<T> dataType;

    @Override
    public BlockTypeId id() {
      throw new UnsupportedOperationException();
    }

    @Override
    public DataStorage<T> parse(ByteInput input) throws IOException {
      return dataType.parseStorage(input);
    }
  }
}
