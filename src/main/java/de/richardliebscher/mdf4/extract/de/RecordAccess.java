/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.extract.de;

import java.io.IOException;

public interface RecordAccess {
    <S extends DeserializeSeed<T>, T> T nextElementSeed(Deserialize<T> deserialize, S seed) throws IOException;

    default <T> T nextElement(Deserialize<T> deserialize) throws IOException {
        return nextElementSeed(deserialize, DeserializeSeed.empty());
    }

    int size();
}
