/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class DataBlock implements Data<DataBlock> {

  long dataPos;
  long dataLength;

  @Override
  public ReadableByteChannel getChannel(ByteInput input) throws IOException {
    input.seek(dataPos);
    return input.getChannel();
  }

  public static DataBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parse(ID, input);
    return new DataBlock(input.pos(), blockHeader.getDataLength());
  }

  public static final Type TYPE = new Type();
  public static final DataContainerType<DataBlock, DataContainer<DataBlock>> CONTAINER_TYPE
      = new DataType.ContainerType<>(TYPE);
  public static final BlockType<DataStorage<DataBlock>> STORAGE_TYPE
      = new DataType.StorageType<>(TYPE);
  public static final BlockTypeId ID = BlockTypeId.of('D', 'T');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements DataType<DataBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public DataBlock parse(ByteInput input) throws IOException {
      return DataBlock.parse(input);
    }
  }
}

