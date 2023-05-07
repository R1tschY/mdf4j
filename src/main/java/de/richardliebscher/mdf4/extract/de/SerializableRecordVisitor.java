/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.Serializable;

/**
 * Serializable visitor to deserialize record.
 *
 * @param <T> Target type
 */
public interface SerializableRecordVisitor<T> extends RecordVisitor<T>, Serializable {

}
