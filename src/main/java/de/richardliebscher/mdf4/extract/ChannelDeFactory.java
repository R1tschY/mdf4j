/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import java.io.IOException;

/**
 * Create deserialization for channels.
 *
 * @param <B> Record builder type
 */
public interface ChannelDeFactory<B> {
  /**
   * Create deserialization for a channel.
   *
   * @param dataGroup Data group
   * @param group     Channel group
   * @param channel   Channel
   * @return Deserialization for channel or {@code null}, when channel should be ignored
   */
  DeserializeInto<B> createDeserialization(DataGroup dataGroup, ChannelGroup group, Channel channel)
      throws IOException;
}
