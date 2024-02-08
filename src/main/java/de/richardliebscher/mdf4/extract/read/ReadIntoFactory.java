/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import de.richardliebscher.mdf4.io.ByteInput;
import java.io.Serializable;

public interface ReadIntoFactory<T> extends Serializable {
  ReadInto<T> build(ByteInput input);
}
