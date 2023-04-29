/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

public interface Deserializer {
    <R> R deserialize_row(RecordVisitor<R> recordVisitor) throws IOException;
    <R> R deserialize_value(Visitor<R> visitor) throws IOException;
}
