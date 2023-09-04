/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.io.ByteInput;
import de.richardliebscher.mdf4.io.FromBytesInput;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
public class ChannelBlockData implements DataRootBlock, UncompressedChannelData {

  long dataPos;
  long dataLength;

  @Override
  public ReadableByteChannel getChannel(ByteInput input) throws IOException {
    input.seek(dataPos);
    return input.getChannel();
  }

  public static ChannelBlockData parse(ByteInput input) throws IOException {
    final var blockHeader = BlockHeader.parse(BlockType.DT, input);
    return new ChannelBlockData(input.pos(), blockHeader.getDataLength());
  }

  public static final Meta META = new Meta();

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Meta implements FromBytesInput<ChannelBlockData> {

    @Override
    public ChannelBlockData parse(ByteInput input) throws IOException {
      return ChannelBlockData.parse(input);
    }
  }
}

