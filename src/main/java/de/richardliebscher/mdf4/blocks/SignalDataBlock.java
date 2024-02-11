/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class SignalDataBlock implements Data<SignalDataBlock> {
  long dataPos;
  long dataLength;

  public ReadableByteChannel getChannel(ByteInput input) throws IOException {
    input.seek(dataPos);
    return input.getChannel();
  }

  @Override
  public long getChannelLength() {
    return dataLength;
  }

  public static SignalDataBlock parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parse(ID, input);
    return new SignalDataBlock(input.pos(), blockHeader.getDataLength());
  }

  public static final Type TYPE = new Type();
  public static final BlockTypeId ID = BlockTypeId.of('S', 'D');

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Type implements BlockType<SignalDataBlock> {

    @Override
    public BlockTypeId id() {
      return ID;
    }

    @Override
    public SignalDataBlock parse(ByteInput input) throws IOException {
      return SignalDataBlock.parse(input);
    }
  }
}
