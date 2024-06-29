/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Mark a type with a built-in integer to contain an unsigned value.
 */
@Target({ElementType.LOCAL_VARIABLE, ElementType.PARAMETER, ElementType.METHOD})
public @interface Unsigned {

}
