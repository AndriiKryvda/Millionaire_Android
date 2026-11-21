#!/usr/bin/env python3
"""Generates the small self-made sound effects used by the game (composeResources/files/sounds/*.wav).

Everything is synthesised locally - no third-party assets, no network access.
Run:  python tools/build_sounds.py
"""
from __future__ import annotations

import math
import os
import struct
import wave

SAMPLE_RATE = 22050
AMPLITUDE = 12000
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "shared", "src", "commonMain", "composeResources", "files", "sounds")


def tone(freq: float, duration: float, decay: float = 6.0, harmonics: float = 0.0) -> list[float]:
    """One decaying sine (optionally with a second harmonic for a richer timbre)."""
    samples = int(SAMPLE_RATE * duration)
    out = []
    for i in range(samples):
        t = i / SAMPLE_RATE
        envelope = math.exp(-decay * t)
        value = math.sin(2 * math.pi * freq * t)
        if harmonics:
            value += harmonics * math.sin(2 * math.pi * freq * 2 * t)
        out.append(value * envelope)
    return out


def join(*parts: list[float]) -> list[float]:
    merged: list[float] = []
    for part in parts:
        merged.extend(part)
    return merged


def sweep(start: float, end: float, duration: float, decay: float = 4.0) -> list[float]:
    samples = int(SAMPLE_RATE * duration)
    out = []
    for i in range(samples):
        t = i / SAMPLE_RATE
        freq = start + (end - start) * (i / samples)
        out.append(math.sin(2 * math.pi * freq * t) * math.exp(-decay * t))
    return out


def write(name: str, samples: list[float]) -> None:
    path = os.path.join(OUT_DIR, f"{name}.wav")
    with wave.open(path, "w") as target:
        target.setnchannels(1)
        target.setsampwidth(2)
        target.setframerate(SAMPLE_RATE)
        frames = bytearray()
        for value in samples:
            frames.extend(struct.pack("<h", int(max(-1.0, min(1.0, value)) * AMPLITUDE)))
        target.writeframes(bytes(frames))
    print(f"  {name}.wav {os.path.getsize(path):>6} bytes")


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    print("Writing sound effects to", os.path.normpath(OUT_DIR))
    write("click", tone(1180.0, 0.05, decay=45.0))
    write("select", join(tone(880.0, 0.06, decay=25.0), tone(1320.0, 0.09, decay=25.0)))
    write(
        "correct",
        join(
            tone(659.0, 0.12, decay=10.0),
            tone(880.0, 0.12, decay=10.0),
            tone(1318.0, 0.28, decay=8.0),
        ),
    )
    write(
        "wrong",
        join(
            sweep(320.0, 180.0, 0.25, decay=4.0),
            sweep(240.0, 110.0, 0.45, decay=4.0),
        ),
    )
    write(
        "win",
        join(
            tone(523.0, 0.16, decay=6.0, harmonics=0.4),
            tone(659.0, 0.16, decay=6.0, harmonics=0.4),
            tone(784.0, 0.16, decay=6.0, harmonics=0.4),
            tone(1046.0, 0.55, decay=4.0, harmonics=0.4),
        ),
    )
    write(
        "walkaway",
        join(
            tone(740.0, 0.16, decay=12.0),
            tone(494.0, 0.34, decay=9.0),
        ),
    )


if __name__ == "__main__":
    main()