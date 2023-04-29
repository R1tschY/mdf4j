/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4;

import de.richardliebscher.mdf4.blocks.Header;
import de.richardliebscher.mdf4.blocks.Id;
import de.richardliebscher.mdf4.io.ByteInput;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class InternalReader {
    @Getter
    private final ByteInput input;
    @Getter
    private final Id id;
    @Getter
    private final Header header;
}
