/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public abstract class FlagsBase<Self extends FlagsBase<Self>> {

  protected final int value;

  protected abstract Self create(int a);

  public Self merge(Self other) {
    return create(this.value | other.value);
  }

  public boolean test(Self test) {
    return (value & test.value) == test.value;
  }

  public boolean anyOf(Self test) {
    return (value & test.value) != 0;
  }

  public boolean isEmpty() {
    return value == 0;
  }
}
