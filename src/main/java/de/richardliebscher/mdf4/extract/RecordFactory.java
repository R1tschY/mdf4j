/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.Channel;
import de.richardliebscher.mdf4.ChannelGroup;
import de.richardliebscher.mdf4.DataGroup;
import de.richardliebscher.mdf4.extract.de.DeserializeInto;
import java.io.IOException;

/**
 * Select channel group and channels.
 *
 * @param <B> Record builder type
 * @param <R> Record type
 */
public interface RecordFactory<B, R> {

  /**
   * Select channel group.
   *
   * <p>
   * Search ends on first channel group that return {@code true}.
   * </p>
   *
   * @param dataGroup Data group
   * @param group     Channel group
   * @return {@code true}, when channel group should be used
   */
  boolean selectGroup(DataGroup dataGroup, ChannelGroup group) throws IOException;

  /**
   * Select channel.
   *
   * @param dataGroup Data group
   * @param group     Channel group
   * @param channel   Channel
   * @return Deserialization for channel or {@code null}, when channel should be ignored
   */
  DeserializeInto<B> selectChannel(DataGroup dataGroup, ChannelGroup group, Channel channel)
      throws IOException;

  B createRecordBuilder();

  R finishRecord(B unfinishedRecord) throws IOException;

}
