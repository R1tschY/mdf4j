/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.blocks.Channel;
import de.richardliebscher.mdf4.blocks.ChannelGroup;
import de.richardliebscher.mdf4.blocks.DataGroup;

/**
 * Select channel group and channels.
 */
public interface ChannelSelector {

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
  boolean selectGroup(DataGroup dataGroup, ChannelGroup group);

  /**
   * Select channel.
   *
   * @param dataGroup Data group
   * @param group     Channel group
   * @param channel   Channel
   * @return {@code true}, when channel should be used
   */
  boolean selectChannel(DataGroup dataGroup, ChannelGroup group, Channel channel);

}
