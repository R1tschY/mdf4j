/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract;

import de.richardliebscher.mdf4.blocks.Channel;
import de.richardliebscher.mdf4.blocks.ChannelGroup;
import de.richardliebscher.mdf4.blocks.DataGroup;

public interface ChannelSelector {
    boolean selectChannel(DataGroup dataGroup, ChannelGroup group, Channel channel);

    boolean selectGroup(DataGroup dataGroup, ChannelGroup group);
}
