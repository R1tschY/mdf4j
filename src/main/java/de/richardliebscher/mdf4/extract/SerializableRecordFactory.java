/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.extract.de.SerializableDeserializeInto;
import java.io.IOException;
import java.io.Serializable;

/**
 * Serializable visitor to deserialize record.
 *
 * @param <B> Record builder type
 * @param <R> Record type
 */
public interface SerializableRecordFactory<B, R> extends RecordFactory<B, R>, Serializable {

  /**
   * Select channel.
   *
   * @param dataGroup Data group
   * @param group     Channel group
   * @param channel   Channel
   * @return {@code true}, when channel should be used
   */
  SerializableDeserializeInto<B> selectChannel(
      DataGroup dataGroup, ChannelGroup group, Channel channel) throws IOException;
}
