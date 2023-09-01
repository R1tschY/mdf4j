/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.Serializable;

/**
 * Serializable variant of {@link DeserializeInto}.
 *
 * @param <T> Structure type
 */
public interface SerializableDeserializeInto<T> extends DeserializeInto<T>, Serializable {
}
