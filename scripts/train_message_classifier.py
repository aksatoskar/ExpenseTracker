#!/usr/bin/env python3
"""Train the on-device TFLite message classifier (8 classes, sender-aware, 1024-dim hash)."""

from __future__ import annotations

import json
import re
from pathlib import Path

import numpy as np
import tensorflow as tf

HASH_DIM = 1024
NUM_CLASSES = 8
LABELS = [
    "actual_debit",
    "future_debit",
    "credit",
    "receipt",
    "otp",
    "reward_cashback",
    "phishing_spam",
    "unknown",
]

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
MODEL_PATH = ASSETS / "message_classifier.tflite"
LABELS_PATH = ASSETS / "message_classifier_labels.json"

# (sender_or_none, message, class_index)
TRAINING_DATA: list[tuple[str | None, str, int]] = [
    # --- Actual debits (0) ---
    ("AD-HDFCBK-S", "INR 250 debited from account XXXX1234 via UPI to SWIGGY", 0),
    ("VK-ICICIB-S", "Rs.499 spent on Debit Card ending 4567 at NETFLIX", 0),
    ("AD-HDFCBK-S", "UPI payment of Rs 230 successful to ZOMATO", 0),
    ("VM-SBIINB-S", "Rs 99 debited for FASTag recharge", 0),
    ("AD-HDFCBK-S", "A/c XXXX2345 debited by Rs 550 for electricity bill payment", 0),
    ("AD-HDFCBK-S", "NACH debit of Rs 1500 processed successfully", 0),
    ("VK-ICICIB-S", "EMI of Rs 8560 deducted from your account", 0),
    ("AD-HDFCBK-S", "Debit card purchase INR 750 at DMART", 0),
    ("VK-AXISBK-S", "Fuel purchase Rs 2000 using card ending 1234", 0),
    ("AD-HDFCBK-S", "UPI transaction successful. Rs 45 paid to blinkit@ybl", 0),
    ("AD-HDFCBK-S", "Rs.899 deducted for Amazon Prime subscription", 0),
    ("VM-SBIINB-S", "Amount Rs.349 debited via NETBANKING", 0),
    ("AD-HDFCBK-S", "Standing instruction executed. Rs.1500 debited", 0),
    ("AD-HDFCBK-S", "Autopay debit of Rs 499 completed", 0),
    ("VM-SBIINB-S", "Rs 180 debited towards mobile recharge", 0),
    ("AD-HDFCBK-S", "Txn successful. INR 99 debited from A/c XX1234 on 20-Jun-25", 0),
    ("AD-HDFCBK-S", "INR 2500 debited from HDFC Bank A/c *4915 on 12/06/25 towards ZOMATO", 0),
    ("VK-ICICIB-S", "Rs 799 spent on CRED Club membership. Available balance Rs 12000", 0),
    ("AD-HDFCBK-S", "Debit alert: Rs 45.00 debited at MERCHANT NAME on 05-06-25", 0),
    ("VM-SBIINB-S", "Rs. 1800 withdrawn at ATM ID 1234 on 01/06/25", 0),
    ("AD-HDFCBK-S", "Rs 75 debited via UPI to blinkit-1@ybl. Bal Rs 4500. HDFC Bank", 0),
    ("AD-HDFCBK-S", "UPI txn of Rs.150 to IRCTC successful. Ref no 5544332211", 0),
    ("PHONEPE", "Rs 1200 paid using PhonePe to AMAZON PAY", 0),
    ("AD-HDFCBK-S", "Purchase using card ending 1234 for Rs 899 at MERCHANT on 12/06/25", 0),
    ("VM-SBIINB-S", "Cash withdrawal of Rs 2000 from ATM on 01/06/25", 0),
    ("VK-AXISBK-S", "Spent Rs.1730.43 on Debit Card ending 4567 at AMAZON", 0),
    (
        "VA-FEDBNK-T",
        "Debited Rs 2.00 from a/c X2225 on 27Jun26 00:20 via UPI to aksatoskar-1. "
        "Ref 617817479920.Bal Rs 11660.38. Not you?Call 18004251199 -Federal Bank",
        0,
    ),
    (
        "VA-FEDBNK-T",
        "Debited Rs 6.00 from a/c X2225 on 25Jun26 23:59 via UPI to aksatoskar-1. "
        "Ref 617625008733.Bal Rs 11662.38. Not you?Call 18004251199 -Federal Bank",
        0,
    ),
    (
        "JM-HDFCBK-T",
        "Sent Rs.3580.00 From HDFC Bank A/C *4915 To SHREEDEVA FOODS On 28/06/26 "
        "Ref 125451627257 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
        0,
    ),
    (
        "JM-HDFCBK-T",
        "Sent Rs.260.00 From HDFC Bank A/C *4915 To KAMLESH C RATHOD On 28/06/26 "
        "Ref 125452087025 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
        0,
    ),
    (
        "AD-HDFCBK-S",
        "Sent Rs.6.00 From HDFC Bank A/C *4915 To MERCHANT On 25/06/26 Ref 654207042327",
        0,
    ),
    (
        "JD-ICICIT-S",
        "ICICI Bank Acct XX678 debited for Rs 1190.00 on 27-Jun-26; SWIGGY credited. "
        "UPI:683699861026. Call 18002662 for dispute.",
        0,
    ),
    (None, "Rs.450 paid to Swiggy via UPI ref 12345", 0),
    (None, "Payment successful. Rs 1200 transferred to AMAZON PAY", 0),
    # --- Future debits (1) ---
    (
        "com.nextbillion.groww",
        "SIP: Instalment due in 2 days ₹20,000.00 will be deducted for "
        "Parag Parikh Flexi Cap Fund Direct Growth. Please ensure sufficient bank balance.",
        1,
    ),
    (
        "com.nextbillion.groww",
        "SIP instalment of Rs 5000 is due in 3 days. Please maintain sufficient balance.",
        1,
    ),
    (None, "Your account will be debited tomorrow for SIP installment", 1),
    (None, "Autopay scheduled for Rs 999 on 01-Jul-26", 1),
    (None, "Upcoming debit of Rs 500 due on 30-Jun", 1),
    (None, "NACH mandate will be presented tomorrow", 1),
    (None, "EMI of Rs 2500 due on 5th July", 1),
    (None, "Subscription renewal scheduled", 1),
    (None, "Scheduled payment of Rs 500 on 30/12/26 will be debited from your account", 1),
    (None, "Your payment of Rs 999 is pending approval and will be debited later", 1),
    (None, "Payment reminder: Rs 1200 due on 15-Aug-26. No debit yet.", 1),
    (None, "E-mandate registered. Auto debit on 05-Jul-26 for Rs 499", 1),
    (None, "Standing instruction: Rs 2000 will be charged on 1st of every month", 1),
    (None, "Your autopay of Rs 999 will be debited on 30-Jun-26 from A/c *1234", 1),
    (None, "Rs 15000 will be deducted from your account in 2 days for mutual fund SIP", 1),
    (None, "Reminder: EMI of Rs 8500 is due in 5 days. No amount debited yet.", 1),
    (None, "Please ensure sufficient balance. Rs 2000 will be withdrawn on 01-Jul-26", 1),
    # --- Credits (2) ---
    (None, "Rs 1500 credited to your account", 2),
    (None, "Salary of Rs 55000 credited", 2),
    (None, "Refund of Rs 999 processed", 2),
    (None, "Cashback Rs 50 credited", 2),
    (None, "Interest of Rs 12 credited", 2),
    (None, "NEFT credit of Rs 10000 received", 2),
    (None, "IMPS credit received", 2),
    (None, "INR 1000 credited to your account on 25/06/26. Available balance Rs 15000", 2),
    # --- Receipts (3) ---
    ("AX-APLPHR-S", "Thank you for shopping with Reliance Retail. Bill amount Rs 650", 3),
    (None, "Download invoice for Rs 1200", 3),
    (None, "Purchase receipt attached. Amount Rs 999", 3),
    (None, "Order confirmed. Total amount Rs 799", 3),
    (None, "Tax invoice generated for Rs 599", 3),
    ("AX-APLPHR-S", "Apollo Pharmacy purchase Rs 521.64", 3),
    (None, "Your order has been delivered. Amount paid Rs 399", 3),
    ("AX-APLPHR-S", "Thank you for your purchase of Rs 450. Download your bill from the app.", 3),
    (None, "Tax invoice for order #12345. Amount Rs 899. Order confirmation attached.", 3),
    # --- OTP (4) ---
    (None, "OTP 123456 for transaction of Rs 500", 4),
    (None, "Do not share OTP 987654", 4),
    (None, "One time password is 456123", 4),
    (None, "Authentication code 778899", 4),
    (None, "Use OTP 1234 to complete payment", 4),
    (None, "OTP 883921 is your one time password for transaction. Do not share.", 4),
    # --- Reward / cashback (5) ---
    (None, "You earned 500 reward points on your last purchase", 5),
    (None, "Cashback earned Rs 25 on your order", 5),
    (None, "Bonus points credited to your loyalty account", 5),
    # --- Phishing / spam (6) ---
    (None, "Update KYC immediately to avoid account block", 6),
    (None, "Click here to unlock your bank account", 6),
    (None, "Your PAN is not linked. Update now", 6),
    (None, "Congratulations! Win Rs 5 lakh", 6),
    (None, "Loan approved instantly. Apply now", 6),
    (None, "Claim your reward points", 6),
    (None, "Account frozen. Contact support on WhatsApp", 6),
    (None, "Your account will be blocked. Verify KYC immediately: http://fake-bank.com", 6),
    (None, "Congratulations! You won Rs 500000 lottery. Click to claim now", 6),
    (None, "Federal Bank alert: unusual activity. Update PAN at bit.ly/fake-link", 6),
    # --- Unknown / weak / mirrors (7) ---
    (
        "com.google.android.apps.messaging",
        "JM-HDFCBK-S Sent Rs.6.00 From HDFC Bank A/C *4915 To AKSHAY MEGHASHYAM SATOSKA "
        "On 25/06/26 Ref 654207042327 Not You? Call 18002586161",
        7,
    ),
    (
        "com.google.android.apps.messaging",
        "VM-HDFCBK-T Sent Rs.4.00 From HDFC Bank A/C *4915 To AKSHAY MEGHASHYAM SATOSKA "
        "On 25/06/26 Ref 654234436997",
        7,
    ),
]


def combined_text(sender: str | None, message: str) -> str:
    sender = (sender or "").strip()
    message = message.strip()
    return f"{sender} {message}".strip() if sender else message


def java_string_hash(value: str) -> int:
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


def augment(sender: str | None, text: str, label: int, repeats: int = 14) -> list[tuple[np.ndarray, int]]:
    base = combined_text(sender, text)
    samples: list[tuple[np.ndarray, int]] = []
    variants = [
        base,
        base.upper(),
        base.replace("Rs.", "Rs"),
        base.replace("Rs", "INR"),
        " ".join(base.split()),
    ]
    for variant in variants:
        samples.append((featurize(variant), label))
    while len(samples) < repeats:
        samples.append((featurize(base), label))
    return samples[:repeats]


def build_dataset() -> tuple[np.ndarray, np.ndarray]:
    xs: list[np.ndarray] = []
    ys: list[int] = []
    for sender, text, label in TRAINING_DATA:
        for features, y in augment(sender, text, label):
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
    LABELS_PATH.write_text(
        json.dumps({"labels": LABELS, "hash_dim": HASH_DIM, "num_classes": NUM_CLASSES}, indent=2)
    )


def main() -> None:
    x, y = build_dataset()
    model = tf.keras.Sequential(
        [
            tf.keras.layers.Input(shape=(HASH_DIM,)),
            tf.keras.layers.Dense(256, activation="relu"),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(128, activation="relu"),
            tf.keras.layers.Dropout(0.15),
            tf.keras.layers.Dense(NUM_CLASSES, activation="softmax"),
        ]
    )
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.004),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
    model.fit(x, y, epochs=200, batch_size=16, verbose=1)
    export_tflite(model)
    print(f"Wrote {MODEL_PATH} ({MODEL_PATH.stat().st_size} bytes)")
    import verify_message_classifier

    verify_message_classifier.main()


if __name__ == "__main__":
    main()
