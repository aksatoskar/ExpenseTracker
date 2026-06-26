#!/usr/bin/env python3
"""Train a small TFLite classifier for bank SMS / notification spam vs valid debits."""

from __future__ import annotations

import json
import re
from pathlib import Path

import numpy as np
import tensorflow as tf

HASH_DIM = 256
NUM_CLASSES = 3
# 0 = valid_debit, 1 = spam, 2 = invalid
LABELS = ["valid_debit", "spam", "invalid"]

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
MODEL_PATH = ASSETS / "message_classifier.tflite"
LABELS_PATH = ASSETS / "message_classifier_labels.json"

TRAINING_DATA: list[tuple[str, int]] = [
    # Valid debits — must include strong debit keywords
    ("Txn successful. INR 99 debited from A/c XX1234 on 20-Jun-25", 0),
    ("INR 2500 debited from HDFC Bank A/c *4915 on 12/06/25 towards ZOMATO. UPI Ref 998877", 0),
    ("Your A/c XX1234 debited with Rs.350.00 on 10-Jun-25. Info: UPI/merchant@paytm", 0),
    ("Rs 799 spent on CRED Club membership. Available balance Rs 12000", 0),
    ("Debit alert: Rs 45.00 debited at MERCHANT NAME on 05-06-25", 0),
    ("Rs. 1800 withdrawn at ATM ID 1234 on 01/06/25", 0),
    ("A/c *7788 debited INR 650 on 18Jun25 for POS purchase at BIG BAZAAR", 0),
    ("Rs 75 debited via UPI to blinkit-1@ybl. Bal Rs 4500. HDFC Bank", 0),
    ("UPI txn of Rs.150 to IRCTC successful. Ref no 5544332211", 0),
    ("Rs 1200 paid using PhonePe to AMAZON PAY", 0),
    ("Purchase using card ending 1234 for Rs 899 at MERCHANT on 12/06/25", 0),
    ("Cash withdrawal of Rs 2000 from ATM on 01/06/25", 0),
    # Spam / phishing
    (
        "Debited Rs 6.00 from a/c X2685 on 25Jun26 23:59 via UPI to aksatoskar-1. "
        "Ref 617625008733.Bal Rs 11662.38. Not you?Call 18004251199 -Federal Bank",
        1,
    ),
    ("Your account will be blocked. Verify KYC immediately: http://fake-bank.com", 1),
    ("Congratulations! You won Rs 500000 lottery. Click to claim now", 1),
    ("OTP 883921 is your one time password for transaction. Do not share.", 1),
    ("Dear customer, your card ending 1234 has been used for Rs 9999. Call 9876543210 if not you", 1),
    ("Urgent: Suspicious login detected. Share OTP to secure account", 1),
    ("Get instant loan of Rs 500000 approved. Apply now limited offer", 1),
    ("FREE Rs 2000 cashback waiting. Tap link to activate your reward", 1),
    ("Federal Bank alert: unusual activity. Update PAN at bit.ly/fake-link", 1),
    ("Rs 1 debited test txn. Not you? Call this number immediately 9999999999", 1),
    ("Your SBI account suspended due to KYC. Contact agent on WhatsApp", 1),
    # Invalid — mirrors, future, receipts, weak debits, credits
    (
        "JM-HDFCBK-S Sent Rs.6.00 From HDFC Bank A/C *4915 To AKSHAY MEGHASHYAM SATOSKA "
        "On 25/06/26 Ref 654207042327 Not You? Call 18002586161",
        2,
    ),
    (
        "VM-HDFCBK-T Sent Rs.4.00 From HDFC Bank A/C *4915 To AKSHAY MEGHASHYAM SATOSKA "
        "On 25/06/26 Ref 654234436997",
        2,
    ),
    ("INR 1000 credited to your account on 25/06/26. Available balance Rs 15000", 2),
    ("Refund of Rs 250 credited to A/c XX1234 on 20-Jun-25", 2),
    ("Cashback Rs 50 credited for your last transaction", 2),
    ("Scheduled payment of Rs 500 on 30/12/26 will be debited from your account", 2),
    ("Your payment of Rs 999 is pending approval and will be debited later", 2),
    ("Payment reminder: Rs 1200 due on 15-Aug-26. No debit yet.", 2),
    ("E-mandate registered. Auto debit on 05-Jul-26 for Rs 499", 2),
    ("Standing instruction: Rs 2000 will be charged on 1st of every month", 2),
    ("Thank you for your purchase of Rs 450. Download your bill from the app.", 2),
    ("Tax invoice for order #12345. Amount Rs 899. Order confirmation attached.", 2),
    ("Rs.450 paid to Swiggy via UPI ref 12345", 2),
    ("Payment successful. Rs 1200 transferred to AMAZON PAY", 2),
    ("You earned 500 reward points on your last purchase", 2),
]


def java_string_hash(value: str) -> int:
    """Match Kotlin String.hashCode() for ASCII tokens."""
    h = 0
    for ch in value:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    if h >= 0x80000000:
        h -= 0x100000000
    return h


def featurize(text: str) -> np.ndarray:
    vector = np.zeros(HASH_DIM, dtype=np.float32)
    tokens = re.findall(r"[a-z0-9]+", text.lower())
    for token in tokens:
        bucket = abs(java_string_hash(token)) % HASH_DIM
        vector[bucket] += 1.0
    norm = np.linalg.norm(vector)
    if norm > 0:
        vector /= norm
    return vector


def augment(text: str, label: int, repeats: int = 12) -> list[tuple[np.ndarray, int]]:
    samples: list[tuple[np.ndarray, int]] = []
    variants = [
        text,
        text.upper(),
        text.replace("Rs.", "Rs"),
        text.replace("Rs", "INR"),
        " ".join(text.split()),
    ]
    for variant in variants:
        samples.append((featurize(variant), label))
    while len(samples) < repeats:
        samples.append((featurize(text), label))
    return samples[:repeats]


def build_dataset() -> tuple[np.ndarray, np.ndarray]:
    xs: list[np.ndarray] = []
    ys: list[int] = []
    for text, label in TRAINING_DATA:
        for features, y in augment(text, label):
            xs.append(features)
            ys.append(y)
    x = np.stack(xs).astype(np.float32)
    y = tf.keras.utils.to_categorical(ys, NUM_CLASSES)
    return x, y


def export_tflite(model: tf.keras.Model) -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    MODEL_PATH.write_bytes(tflite_model)
    LABELS_PATH.write_text(json.dumps({"labels": LABELS, "hash_dim": HASH_DIM}, indent=2))


def main() -> None:
    x, y = build_dataset()
    model = tf.keras.Sequential(
        [
            tf.keras.layers.Input(shape=(HASH_DIM,)),
            tf.keras.layers.Dense(64, activation="relu"),
            tf.keras.layers.Dropout(0.1),
            tf.keras.layers.Dense(32, activation="relu"),
            tf.keras.layers.Dense(NUM_CLASSES, activation="softmax"),
        ]
    )
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.01),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
    model.fit(x, y, epochs=120, batch_size=16, verbose=1)
    export_tflite(model)
    print(f"Wrote {MODEL_PATH} ({MODEL_PATH.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
