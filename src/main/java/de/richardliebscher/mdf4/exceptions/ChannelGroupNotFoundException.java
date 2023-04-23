/**
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.exceptions;

public class ChannelGroupNotFoundException extends Exception {
    public ChannelGroupNotFoundException(String message) {
        super(message);
    }

    public ChannelGroupNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChannelGroupNotFoundException(Throwable cause) {
        super(cause);
    }
}
