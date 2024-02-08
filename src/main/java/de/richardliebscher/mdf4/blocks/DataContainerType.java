/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Copyright 2024 Richard Liebscher <r1tschy@posteo.de>
 */

package de.richardliebscher.mdf4.blocks;

public interface DataContainerType<B extends Data<B>, T extends DataContainer<B>> extends
    BlockType<T> {
}
