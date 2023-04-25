/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.exceptions;

import de.richardliebscher.mdf4.MdfFormatVersion;
import lombok.Getter;

public class VersionException extends ParseException {
    @Getter
    private final MdfFormatVersion unsupportedVersion;

    public VersionException(MdfFormatVersion unsupportedVersion) {
        super("Unsupported major version: " + unsupportedVersion.getMajor());
        this.unsupportedVersion = unsupportedVersion;
    }
}
