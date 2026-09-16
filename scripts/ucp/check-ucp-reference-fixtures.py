#!/usr/bin/env python3
"""Validate released UCP scaffolds with a pinned, locally built reference CLI in Docker."""
from __future__ import annotations

import argparse
import subprocess
from pathlib import Path

PIN = "a0fc4fc189add7da533c981bdb3d8a36950e0602"
ROOT = Path(__file__).resolve().parents[2]
BUNDLE = ROOT / "modules/ucp/src/test/resources/ucp-contract/2026-08-25"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--validator", required=True, type=Path, help="local ucp-schema checkout with target/debug/ucp-schema built")
    args = parser.parse_args()
    checkout = args.validator.resolve()
    commit = subprocess.check_output(["git", "-C", str(checkout), "rev-parse", "HEAD"], text=True).strip()
    dirty = subprocess.check_output(["git", "-C", str(checkout), "status", "--porcelain", "--untracked-files=no"], text=True).strip()
    if commit != PIN or dirty:
        raise SystemExit("validator must be a clean checkout of " + PIN)

    cases = []
    for capability, operations in (("cart", ("create", "update")), ("checkout", ("create", "update", "complete"))):
        for operation in operations:
            cases.append((f"shopping_{capability}_request_{operation}", f"shopping/{capability}", "request", operation))
        cases.append((f"shopping_{capability}_response", f"shopping/{capability}", "response", "read"))
    for capability, operation in (("shopping/catalog_lookup", "lookup"), ("shopping/catalog_search", "search"), ("common/location_lookup", "lookup"), ("common/location_search", "search")):
        for direction in ("request", "response"):
            cases.append((f"{capability.replace('/', '_')}_{direction}", capability, direction, operation))
    for direction in ("request", "response"):
        cases.append((f"shopping_catalog_lookup_{direction}_get_product", "shopping/catalog_lookup", direction, "get_product"))
    cases.append(("shopping_order_response", "shopping/order", "response", "read"))

    for fixture, schema, direction, operation in cases:
        command = [
            "docker", "run", "--rm", "--network", "none",
            "-v", f"{checkout.as_posix()}:/tool:ro",
            "-v", f"{BUNDLE.as_posix()}:/bundle:ro", "rust:1.89",
            "/tool/target/debug/ucp-schema", "validate", f"/bundle/scaffolds/{fixture}.json",
            "--schema", f"/bundle/schemas/{schema}.json", f"--{direction}", "--op", operation,
            "--schema-local-base", "/bundle/schemas", "--schema-remote-base", "https://ucp.dev/schemas", "--json",
        ]
        print(f"Validating {fixture}: {direction}/{operation}", flush=True)
        subprocess.run(command, check=True)
    print(f"OK: {len(cases)} released scaffolds validated offline with ucp-schema {PIN}")


if __name__ == "__main__":
    main()
