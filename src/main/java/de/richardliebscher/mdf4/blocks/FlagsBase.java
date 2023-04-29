/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public abstract class FlagsBase<SELF extends FlagsBase<SELF>> {

  protected final int value;

  protected abstract SELF create(int a);

  public SELF merge(SELF other) {
    return create(this.value | other.value);
  }

  public boolean test(SELF test) {
    return (value & test.value) == test.value;
  }

  public boolean isEmpty() {
    return value == 0;
  }
}
