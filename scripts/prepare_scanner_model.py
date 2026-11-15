#!/usr/bin/env python3
"""Install a separately licensed, checksum-verified seven-segment model export."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("model_dir", type=Path,
                        help="Export containing detector.tflite, detector.json, and MODEL_LICENSE")
    args = parser.parse_args()
    source = args.model_dir.resolve()
    names = ("detector.tflite", "detector.json", "MODEL_LICENSE")
    for name in names:
        if not (source / name).is_file():
            parser.error(f"Missing {source / name}")
    metadata = json.loads((source / "detector.json").read_text())
    checksum = hashlib.sha256((source / "detector.tflite").read_bytes()).hexdigest()
    if checksum != metadata.get("model_sha256"):
        parser.error("detector.tflite does not match model_sha256 in detector.json")
    if metadata.get("box_units") != "input_pixels" or not metadata.get("names"):
        parser.error("Expected input_pixels boxes and the detector's class names")
    destination = Path(__file__).resolve().parents[1] / "app/src/main/assets/seven-digits"
    destination.mkdir(parents=True, exist_ok=True)
    if source != destination:
        for name in names:
            shutil.copy2(source / name, destination / name)
    print(f"Prepared scanner model: {checksum}\nAssets: {destination}")


if __name__ == "__main__":
    main()
