#!/usr/bin/env python3
"""Reproducibly verify the pinned UCP Phase 0 contract bundle without network access."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path, PurePosixPath
from typing import Any

REPOSITORY = "https://github.com/Universal-Commerce-Protocol/ucp"
TAG = "v2026-08-25"
COMMIT = "cd78fb38e819de77d9b527d110476eccb876f1bd"
ROOT = Path(__file__).resolve().parents[2]
BUNDLE = ROOT / "modules/ucp/src/test/resources/ucp-contract/2026-08-25"
METADATA_FILES = {"manifest.json", "provenance.json"}


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def sha256_bytes(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def read_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"invalid JSON: {path.relative_to(BUNDLE)} ({exc})")


def upstream_bytes(upstream: Path, source_path: str) -> bytes:
    try:
        return subprocess.check_output(
            ["git", "-C", str(upstream), "show", f"{COMMIT}:{source_path}"],
            stderr=subprocess.PIPE,
        )
    except (OSError, subprocess.CalledProcessError) as exc:
        detail = exc.stderr.decode(errors="replace").strip() if isinstance(exc, subprocess.CalledProcessError) else str(exc)
        fail(f"cannot read pinned upstream artifact {source_path}: {detail}")


def source_path_for(rel: str) -> str:
    if rel == "LICENSE":
        return rel
    for bundle_prefix, source_prefix in (
        ("schemas/", "source/schemas/"),
        ("services/", "source/services/"),
        ("scaffolds/", "scripts/scaffolds/"),
    ):
        if rel.startswith(bundle_prefix):
            return source_prefix + rel.removeprefix(bundle_prefix)
    fail(f"cannot map bundle artifact to canonical upstream source: {rel}")


def upstream_files(upstream: Path, source_directory: str) -> set[str]:
    try:
        listing = subprocess.check_output(
            ["git", "-C", str(upstream), "ls-tree", "-r", "--name-only", COMMIT, "--", source_directory], text=True
        )
    except (OSError, subprocess.CalledProcessError) as exc:
        fail(f"cannot list pinned upstream directory {source_directory}: {exc}")
    return {line for line in listing.splitlines() if line}


def bundle_artifacts() -> set[str]:
    return {
        path.relative_to(BUNDLE).as_posix()
        for path in BUNDLE.rglob("*")
        if path.is_file() and path.relative_to(BUNDLE).as_posix() not in METADATA_FILES
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--upstream",
        type=Path,
        default=os.environ.get("UCP_UPSTREAM"),
        help="local checkout of the pinned UCP repository (or set UCP_UPSTREAM)",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.upstream is None:
        fail("provide --upstream PATH or set UCP_UPSTREAM; no upstream path is hard-coded")
    upstream = args.upstream.resolve()
    if not upstream.is_dir():
        fail(f"upstream path is not a directory: {upstream}")

    manifest_path = BUNDLE / "manifest.json"
    provenance_path = BUNDLE / "provenance.json"
    if not manifest_path.is_file() or not provenance_path.is_file():
        fail("bundle manifest or provenance is missing")
    manifest = read_json(manifest_path)
    provenance = read_json(provenance_path)
    for data, label in ((manifest, "manifest"), (provenance, "provenance")):
        if data.get("protocol_version") != "2026-08-25":
            fail(f"{label} protocol version is not 2026-08-25")
        if data.get("source_repository") != REPOSITORY or data.get("source_tag") != TAG:
            fail(f"{label} source repository or tag differs from the pin")
        if data.get("source_commit") != COMMIT:
            fail(f"{label} source commit differs from the pin")
    try:
        actual_commit = subprocess.check_output(
            ["git", "-C", str(upstream), "rev-parse", f"{COMMIT}^{{commit}}"], text=True
        ).strip()
    except (OSError, subprocess.CalledProcessError) as exc:
        fail(f"cannot resolve pinned local upstream commit: {exc}")
    if actual_commit != COMMIT:
        fail("local upstream does not resolve to the pinned commit")

    entries = manifest.get("files")
    if not isinstance(entries, list) or not entries:
        fail("manifest does not list imported files")
    expected: dict[str, dict[str, str]] = {}
    for entry in entries:
        if not isinstance(entry, dict):
            fail("manifest contains a non-object file entry")
        rel, digest = entry.get("path"), entry.get("sha256")
        if not all(isinstance(value, str) and value for value in (rel, digest)):
            fail("every manifest entry requires path and sha256")
        source = entry.get("source_path", source_path_for(rel))
        if not isinstance(source, str) or not source:
            fail(f"invalid source_path for {rel}")
        normalized = PurePosixPath(rel)
        if normalized.is_absolute() or ".." in normalized.parts or normalized.as_posix() != rel:
            fail(f"unsafe manifest bundle path: {rel}")
        if rel in expected:
            fail(f"duplicate manifest path: {rel}")
        expected[rel] = {"sha256": digest, "source_path": source}

    manifest_sources = {entry["source_path"] for entry in expected.values()}
    required_sources = {"LICENSE"}
    required_sources.update(upstream_files(upstream, "source/schemas"))
    required_sources.update(upstream_files(upstream, "scripts/scaffolds"))
    required_sources.update({"source/services/common/rest.openapi.json", "source/services/shopping/rest.openapi.json"})
    missing_sources = sorted(required_sources - manifest_sources)
    unexpected_sources = sorted(manifest_sources - required_sources)
    if missing_sources:
        fail(f"pinned upstream artifacts omitted from manifest: {', '.join(missing_sources)}")
    if unexpected_sources:
        fail(f"manifest source paths are outside the declared import scope: {', '.join(unexpected_sources)}")

    actual = bundle_artifacts()
    missing = sorted(set(expected) - actual)
    untracked = sorted(actual - set(expected))
    if missing:
        fail(f"manifest files missing from bundle: {', '.join(missing)}")
    if untracked:
        fail(f"untracked bundle artifacts: {', '.join(untracked)}")

    for rel, entry in expected.items():
        path = BUNDLE / rel
        content = path.read_bytes()
        if sha256_bytes(content) != entry["sha256"]:
            fail(f"manifest hash mismatch: {rel}")
        if content != upstream_bytes(upstream, entry["source_path"]):
            fail(f"imported bytes differ from pinned upstream: {rel} <- {entry['source_path']}")
        if path.suffix == ".json":
            read_json(path)

    for root in manifest.get("contract_roots", []):
        if root not in expected:
            fail(f"contract root omitted from manifest: {root}")
    for service in manifest.get("service_descriptions", []):
        if service not in expected:
            fail(f"service description omitted from manifest: {service}")
    if manifest.get("license_file") not in expected:
        fail("license file is omitted from manifest")
    print(f"OK: {len(expected)} pinned UCP artifacts byte-verified against {COMMIT}")


if __name__ == "__main__":
    main()
