# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: Copyright 2023 Richard Liebscher <r1tschy@posteo.de>

import numpy as np
from asammdf import MDF, Signal
from pathlib import Path

root = Path(__file__).parent.parent.parent / "resources"

f32_info = np.finfo(np.float32)
f64_info = np.finfo(np.float64)


def primitives():
  timestamps = np.array([0, 1, 2], dtype=np.float32)

  signals = [
    Signal(
        samples=np.array([0, 2 ** 7 - 1, -2 ** 7], dtype=np.int8),
        timestamps=timestamps,
        name='i8'),
    Signal(
        samples=np.array([0, 2 ** 15 - 1, -2 ** 15], dtype=np.int16),
        timestamps=timestamps,
        name='i16'),
    Signal(
        samples=np.array([0, 2 ** 31 - 1, -2 ** 31], dtype=np.int32),
        timestamps=timestamps,
        name='i32'),
    Signal(
        samples=np.array([0, 2 ** 63 - 1, -2 ** 63], dtype=np.int64),
        timestamps=timestamps,
        name='i64'),
    Signal(
        samples=np.array([0, 1, 2 ** 8 - 1], dtype=np.uint8),
        timestamps=timestamps,
        name='u8'),
    Signal(
        samples=np.array([0, 1, 2 ** 16 - 1], dtype=np.uint16),
        timestamps=timestamps,
        name='u16'),
    Signal(
        samples=np.array([0, 1, 2 ** 32 - 1], dtype=np.uint32),
        timestamps=timestamps,
        name='u32'),
    Signal(
        samples=np.array([0, 1, 2 ** 64 - 1], dtype=np.uint64),
        timestamps=timestamps,
        name='u64'),
    Signal(
        samples=np.array([0, f32_info.max, float("inf")], dtype=np.float32),
        timestamps=timestamps,
        name='f32'),
    Signal(
        samples=np.array([0, f64_info.max, float("inf")], dtype=np.float64),
        timestamps=timestamps,
        name='f64'),
  ]

  with MDF(version="4.10") as mdf:
    mdf.append(signals)
    mdf.save(root / "primitives.mf4", overwrite=True)

  with MDF(name=root / "primitives.mf4", version="4.10") as mdf:
    mdf.export(fmt="csv", filename=root / "primitives.csv")


if __name__ == '__main__':
  primitives()
