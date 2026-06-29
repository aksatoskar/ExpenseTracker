#!/usr/bin/env python3
"""Verify TFLite predictions for critical debit vs future-debit cases."""

from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import tensorflow as tf

ROOT = Path(__file__).resolve().parents[1]
MODEL_PATH = ROOT / "app" / "src" / "main" / "assets" / "message_classifier.tflite"

sys.path.insert(0, str(ROOT / "scripts"))
import train_message_classifier as train  # noqa: E402

LABELS = train.LABELS
HASH_DIM = train.HASH_DIM

CASES: list[tuple[str | None, str, str]] = [
    (
        "VA-FEDBNK-T",
        "Debited Rs 2.00 from a/c X2685 on 27Jun26 00:20 via UPI to aksatoskar-1. "
        "Ref 617817479920.Bal Rs 11660.38. Not you?Call 18004251199 -Federal Bank",
        "actual_debit",
    ),
    (
        "JM-HDFCBK-T",
        "Sent Rs.3580.00 From HDFC Bank A/C *4915 To SHREEDEVA FOODS On 28/06/26 "
        "Ref 125451627257 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
        "actual_debit",
    ),
    (
        "com.nextbillion.groww",
        "SIP: Instalment due in 2 days ₹20,000.00 will be deducted for "
        "Parag Parikh Flexi Cap Fund Direct Growth. Please ensure sufficient bank balance.",
        "future_debit",
    ),
    (
        None,
        "Your autopay of Rs 999 will be debited on 30-Jun-26 from A/c *1234",
        "future_debit",
    ),
    (
        "VK-AXISBK-S",
        "Spent Rs.1730.43 on Debit Card ending 4567 at AMAZON",
        "actual_debit",
    ),
]


def predict(interpreter: tf.lite.Interpreter, sender: str | None, message: str) -> tuple[str, int]:
    text = train.combined_text(sender, message)
    features = train.featurize(text).reshape(1, HASH_DIM).astype(np.float32)
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    interpreter.set_tensor(input_details[0]["index"], features)
    interpreter.invoke()
    scores = interpreter.get_tensor(output_details[0]["index"])[0]
    best = int(np.argmax(scores))
    confidence = int(scores[best] * 100)
    return LABELS[best], confidence


def main() -> None:
    if not MODEL_PATH.exists():
        raise SystemExit(f"Missing model: {MODEL_PATH}")

    interpreter = tf.lite.Interpreter(model_path=str(MODEL_PATH))
    interpreter.allocate_tensors()

    failed = 0
    for sender, message, expected in CASES:
        label, confidence = predict(interpreter, sender, message)
        ok = label == expected
        status = "OK" if ok else "FAIL"
        print(f"[{status}] expected={expected} got={label} ({confidence}%) :: {message[:72]}...")
        if not ok:
            failed += 1

    if failed:
        raise SystemExit(f"{failed} verification case(s) failed")
    print(f"All {len(CASES)} verification cases passed.")


if __name__ == "__main__":
    main()
