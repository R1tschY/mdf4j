/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.read;

import java.io.Serializable;

public interface SerializableReadInto<T> extends ReadInto<T>, Serializable {

}
