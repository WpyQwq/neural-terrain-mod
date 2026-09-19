#!/usr/bin/env python3
"""Train and export the small terrain MLP used by the Fabric mod.

Input is JSONL. Each line must contain:
  {"seed": 1, "x": 0, "z": 0, "surface_y": 72,
   "moisture": 0.5, "temperature": 0.6}

The exporter writes format=1, which is consumed by JsonTerrainModel.java.
This script intentionally uses only the Python standard library so a model can
be trained without installing NumPy, PyTorch, or ONNX Runtime.
"""

from __future__ import annotations

import argparse
import json
import math
import random
from pathlib import Path


def features(seed: int, x: int, z: int) -> list[float]:
    folded = (seed ^ (seed >> 33) ^ (seed << 11)) & ((1 << 64) - 1)
    if folded >= (1 << 63):
        folded -= 1 << 64
    offset = folded * 0.00000017
    return [
        math.sin(x * 0.0041),
        math.cos(x * 0.0041),
        math.sin(z * 0.0037),
        math.cos(z * 0.0037),
        math.sin((x + z) * 0.0023),
        math.cos((x - z) * 0.0027),
        math.sin(x * 0.013 + z * 0.009),
        math.cos(x * 0.011 - z * 0.015),
        math.sin((x + offset) * 0.00091),
        math.cos((z - offset) * 0.00107),
    ]


def sigmoid(value: float) -> float:
    value = max(-30.0, min(30.0, value))
    return 1.0 / (1.0 + math.exp(-2.0 * value))


def cave_features(seed: int, x: int, y: int, z: int) -> list[float]:
    folded = (seed ^ (seed >> 29) ^ (seed << 17)) & ((1 << 64) - 1)
    if folded >= (1 << 63):
        folded -= 1 << 64
    offset = folded * 0.00000011
    x_value = x + offset
    z_value = z - offset
    return [
        math.sin(x_value * 0.035),
        math.cos(x_value * 0.035),
        math.sin(z_value * 0.037),
        math.cos(z_value * 0.037),
        math.sin(y * 0.12),
        math.cos(y * 0.12),
        math.sin((x_value + z_value + y) * 0.018),
        math.cos((x_value - z_value + y) * 0.021),
    ]


class Model:
    def __init__(self, hidden: int, rng: random.Random):
        self.hidden = hidden
        scale = math.sqrt(2.0 / (10 + hidden))
        self.hw = [[rng.uniform(-scale, scale) for _ in range(10)] for _ in range(hidden)]
        self.hb = [0.0 for _ in range(hidden)]
        self.ow = [[rng.uniform(-scale, scale) for _ in range(hidden)] for _ in range(3)]
        self.ob = [0.0, 0.0, 0.0]

    def forward(self, inputs: list[float]) -> tuple[list[float], list[float]]:
        hidden_values = []
        for row, bias in zip(self.hw, self.hb):
            hidden_values.append(math.tanh(sum(w * x for w, x in zip(row, inputs)) + bias))
        raw = [sum(w * h for w, h in zip(row, hidden_values)) + bias for row, bias in zip(self.ow, self.ob)]
        outputs = [math.tanh(raw[0]), sigmoid(raw[1]), sigmoid(raw[2])]
        return hidden_values, outputs

    def train_one(self, inputs: list[float], target: list[float], learning_rate: float) -> float:
        hidden_values, outputs = self.forward(inputs)
        loss = sum((out - goal) ** 2 for out, goal in zip(outputs, target)) / 3.0

        output_delta = [
            2.0 * (outputs[0] - target[0]) * (1.0 - outputs[0] ** 2),
            2.0 * (outputs[1] - target[1]) * 2.0 * outputs[1] * (1.0 - outputs[1]),
            2.0 * (outputs[2] - target[2]) * 2.0 * outputs[2] * (1.0 - outputs[2]),
        ]
        hidden_delta = []
        for index, value in enumerate(hidden_values):
            downstream = sum(self.ow[output][index] * output_delta[output] for output in range(3))
            hidden_delta.append(downstream * (1.0 - value * value))

        for output in range(3):
            for index in range(self.hidden):
                self.ow[output][index] -= learning_rate * output_delta[output] * hidden_values[index]
            self.ob[output] -= learning_rate * output_delta[output]
        for index in range(self.hidden):
            for feature in range(10):
                self.hw[index][feature] -= learning_rate * hidden_delta[index] * inputs[feature]
            self.hb[index] -= learning_rate * hidden_delta[index]
        return loss

    def export(self, base_height: float, height_amplitude: float, cave_model: "CaveModel | None" = None) -> dict:
        payload = {
            "format": 1,
            "inputs": 10,
            "hidden": self.hidden,
            "hidden_weights": self.hw,
            "hidden_bias": self.hb,
            "height_weights": self.ow[0],
            "moisture_weights": self.ow[1],
            "temperature_weights": self.ow[2],
            "height_bias": self.ob[0],
            "moisture_bias": self.ob[1],
            "temperature_bias": self.ob[2],
            "base_height": base_height,
            "height_amplitude": height_amplitude,
        }
        if cave_model is not None:
            payload.update(cave_model.export())
        return payload


class CaveModel:
    def __init__(self, hidden: int, rng: random.Random):
        self.hidden = hidden
        scale = math.sqrt(2.0 / (8 + hidden))
        self.hw = [[rng.uniform(-scale, scale) for _ in range(8)] for _ in range(hidden)]
        self.hb = [0.0 for _ in range(hidden)]
        self.ow = [rng.uniform(-scale, scale) for _ in range(hidden)]
        self.ob = 0.0

    def forward(self, inputs: list[float]) -> tuple[list[float], float]:
        hidden_values = [
            math.tanh(sum(w * x for w, x in zip(row, inputs)) + bias)
            for row, bias in zip(self.hw, self.hb)
        ]
        output = sigmoid(sum(w * h for w, h in zip(self.ow, hidden_values)) + self.ob)
        return hidden_values, output

    def train_one(self, inputs: list[float], target: float, learning_rate: float) -> float:
        hidden_values, output = self.forward(inputs)
        loss = (output - target) ** 2
        output_delta = 2.0 * (output - target) * 2.0 * output * (1.0 - output)
        hidden_delta = [
            self.ow[index] * output_delta * (1.0 - value * value)
            for index, value in enumerate(hidden_values)
        ]
        for index in range(self.hidden):
            self.ow[index] -= learning_rate * output_delta * hidden_values[index]
        self.ob -= learning_rate * output_delta
        for index in range(self.hidden):
            for feature in range(8):
                self.hw[index][feature] -= learning_rate * hidden_delta[index] * inputs[feature]
            self.hb[index] -= learning_rate * hidden_delta[index]
        return loss

    def export(self) -> dict:
        return {
            "cave_hidden": self.hidden,
            "cave_hidden_weights": self.hw,
            "cave_hidden_bias": self.hb,
            "cave_weights": self.ow,
            "cave_bias": self.ob,
        }


def load_samples(path: Path, base_height: float, height_amplitude: float) -> list[tuple[list[float], list[float]]]:
    samples = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            continue
        row = json.loads(line)
        required = ("seed", "x", "z", "surface_y", "moisture", "temperature")
        missing = [key for key in required if key not in row]
        if missing:
            raise ValueError(f"line {line_number}: missing {', '.join(missing)}")
        height = max(-1.0, min(1.0, (float(row["surface_y"]) - base_height) / height_amplitude))
        samples.append((features(int(row["seed"]), int(row["x"]), int(row["z"])), [
            height,
            max(0.0, min(1.0, float(row["moisture"]))),
            max(0.0, min(1.0, float(row["temperature"]))),
        ]))
    if not samples:
        raise ValueError("training file contains no samples")
    return samples


def load_cave_samples(path: Path) -> list[tuple[list[float], float]]:
    samples = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            continue
        row = json.loads(line)
        required = ("seed", "x", "y", "z", "cave")
        missing = [key for key in required if key not in row]
        if missing:
            raise ValueError(f"cave line {line_number}: missing {', '.join(missing)}")
        samples.append((
            cave_features(int(row["seed"]), int(row["x"]), int(row["y"]), int(row["z"])),
            max(0.0, min(1.0, float(row["cave"]))),
        ))
    if not samples:
        raise ValueError("cave training file contains no samples")
    return samples


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path, help="JSONL training samples")
    parser.add_argument("--output", required=True, type=Path, help="model JSON output")
    parser.add_argument("--epochs", type=int, default=200)
    parser.add_argument("--hidden", type=int, default=24)
    parser.add_argument("--lr", type=float, default=0.01)
    parser.add_argument("--seed", type=int, default=1234)
    parser.add_argument("--base-height", type=float, default=68.0)
    parser.add_argument("--height-amplitude", type=float, default=46.0)
    parser.add_argument("--cave-input", type=Path, help="optional JSONL cave samples")
    parser.add_argument("--cave-hidden", type=int, default=16)
    parser.add_argument("--cave-epochs", type=int, default=200)
    parser.add_argument("--cave-lr", type=float, default=0.01)
    args = parser.parse_args()

    if args.hidden < 1 or args.hidden > 256:
        parser.error("--hidden must be between 1 and 256")
    samples = load_samples(args.input, args.base_height, args.height_amplitude)
    rng = random.Random(args.seed)
    model = Model(args.hidden, rng)

    for epoch in range(1, args.epochs + 1):
        rng.shuffle(samples)
        loss = sum(model.train_one(inputs, target, args.lr) for inputs, target in samples) / len(samples)
        if epoch == 1 or epoch % 10 == 0 or epoch == args.epochs:
            print(f"epoch={epoch:04d} loss={loss:.6f}")

    cave_model = None
    if args.cave_input is not None:
        if args.cave_hidden < 1 or args.cave_hidden > 128:
            parser.error("--cave-hidden must be between 1 and 128")
        cave_samples = load_cave_samples(args.cave_input)
        cave_model = CaveModel(args.cave_hidden, rng)
        for epoch in range(1, args.cave_epochs + 1):
            rng.shuffle(cave_samples)
            loss = sum(cave_model.train_one(inputs, target, args.cave_lr) for inputs, target in cave_samples) / len(cave_samples)
            if epoch == 1 or epoch % 10 == 0 or epoch == args.cave_epochs:
                print(f"cave_epoch={epoch:04d} cave_loss={loss:.6f}")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(model.export(args.base_height, args.height_amplitude, cave_model), indent=2), encoding="utf-8")
    print(f"wrote {args.output} with {len(samples)} samples")


if __name__ == "__main__":
    main()
