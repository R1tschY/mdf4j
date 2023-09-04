/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.internal;

import de.richardliebscher.mdf4.blocks.Header;
import de.richardliebscher.mdf4.blocks.IdBlock;
import de.richardliebscher.mdf4.io.ByteInput;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternalReader {

  @Getter
  private final ByteInput input;
  @Getter
  private final IdBlock idBlock;
  @Getter
  private final Header header;
}
