/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import de.richardliebscher.mdf4.io.ByteInput;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public interface DataStorage<T extends Data<T>> {
  ReadableByteChannel getChannel(ByteInput input) throws IOException;
}
