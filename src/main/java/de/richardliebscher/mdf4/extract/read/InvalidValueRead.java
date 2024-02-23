/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.extract.de.Visitor;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InvalidValueRead implements ValueRead {

  @Override
  public <T, P> T read(RecordBuffer input, Visitor<T, P> visitor, P param) throws IOException {
    return visitor.visitInvalid(param);
  }
}
