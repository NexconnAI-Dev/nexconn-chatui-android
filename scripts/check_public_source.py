#!/usr/bin/env python3
"""Validate a Nexconn ChatUI public source tree."""

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import List, Optional


REQUIRED_FILES = (
    ".gitignore",
    ".github/workflows/android.yml",
    "CHANGELOG.md",
    "CONTRIBUTING.md",
    "LICENSE",
    "NOTICE",
    "README.md",
    "RELEASE_METADATA.json",
    "SECURITY.md",
    "SOURCE_COMMIT",
    "build.gradle",
    "docs/source-integration.md",
    "docs/version-compatibility.md",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "gradlew",
    "scripts/check_public_source.py",
    "settings.gradle",
)
FORBIDDEN_NAMES = {
    ".git",
    "build",
    "output",
    "nexconn-chat",
    "nexconn-demo",
    "pushtest",
    "sample",
    "samples",
}
FORBIDDEN_CREDENTIAL_NAMES = {
    ".env",
    "agconnect-services.json",
    "credentials.json",
    "google-services.json",
    "id_ed25519",
    "id_rsa",
}
FORBIDDEN_CREDENTIAL_SUFFIXES = {
    ".gpg",
    ".jks",
    ".key",
    ".keystore",
    ".p12",
    ".pem",
    ".pfx",
    ".pgp",
}
LEGACY_WORD = "".join(("ro", "ng"))
LEGACY_BRAND = "".join((LEGACY_WORD, "cl", "oud"))
LEGACY_UPPER_WORD = "".join(("RO", "NG"))
LEGACY_TITLE_WORD = "".join(("Ro", "ng"))
LEGACY_SHORT_PREFIX = "".join(("R", "C"))
LEGACY_CN_BRAND = "\u878d\u4e91"
FORMER_DEMO_PARTS = ("".join(("se", "al")), "".join(("ta", "lk")))
PRIVATE_KEY_TEXT = re.compile(
    r"BEGIN (?:PGP |RSA |OPENSSH |EC )?PRIVATE KEY(?: BLOCK)?",
    re.IGNORECASE,
)
LEGACY_IDENTITY_TEXT = (
    re.compile(re.escape(LEGACY_BRAND), re.IGNORECASE),
    re.compile(
        r"(?<![A-Za-z0-9])" + re.escape(LEGACY_WORD) + r"(?![A-Za-z0-9])",
        re.IGNORECASE,
    ),
    re.compile(
        r"(?<![A-Za-z0-9])" + re.escape(LEGACY_UPPER_WORD) + r"[A-Z0-9_]+"
    ),
    re.compile(
        r"(?<![A-Za-z0-9])"
        + re.escape(LEGACY_TITLE_WORD)
        + r"[A-Z0-9][A-Za-z0-9_]*"
    ),
    re.compile(
        r"(?<![A-Za-z0-9])"
        + re.escape(LEGACY_SHORT_PREFIX)
        + r"[._:][A-Za-z0-9_]+",
        re.IGNORECASE,
    ),
    re.compile(
        r"(?<![A-Za-z0-9])"
        + r"(?:RC|Rc|rc)"
        + r"[A-Z0-9][A-Za-z0-9_]*"
    ),
    re.compile(re.escape(LEGACY_CN_BRAND)),
    re.compile(
        re.escape(FORMER_DEMO_PARTS[0])
        + r"(?:[\s_-]*)"
        + re.escape(FORMER_DEMO_PARTS[1]),
        re.IGNORECASE,
    ),
)
HAN_TEXT = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")
LOCALIZED_HAN_FILES = {
    Path("nexconn-chatui/src/main/res/values/strings.xml"),
}
MANIFEST_COMPATIBILITY_PATH = Path("nexconn-chatui/src/main/AndroidManifest.xml")
STREAM_COMPATIBILITY_PATH = Path(
    "nexconn-chatui/src/main/java/ai/nexconn/chatui/utils/message/StreamMsgUtil.java"
)
MANIFEST_COMPATIBILITY_TEXT = 'android:name="' + "".join(
    ("rc", ".", "nexconnchat")
) + '"'
STREAM_COMPATIBILITY_TEXT = (
    '"' + "".join((LEGACY_SHORT_PREFIX, "_Ext_", "StreamMsgSummary")) + '"'
)
REQUIRED_COMPATIBILITY_TEXT = {
    MANIFEST_COMPATIBILITY_PATH: (MANIFEST_COMPATIBILITY_TEXT,),
    STREAM_COMPATIBILITY_PATH: (STREAM_COMPATIBILITY_TEXT,),
}
ALLOWED_LEGACY_TEXT = {
    MANIFEST_COMPATIBILITY_PATH: (MANIFEST_COMPATIBILITY_TEXT,),
    STREAM_COMPATIBILITY_PATH: (STREAM_COMPATIBILITY_TEXT,),
    Path(
        "nexconn-chatui/src/main/java/ai/nexconn/chatui/utils/text/CharacterParser.java"
    ): ('"' + LEGACY_WORD + '"',),
}
SECRET_ASSIGNMENT = re.compile(
    r"(?im)^\s*(?:def\s+)?(?:[A-Z0-9_.-]*(?:PASSWORD|TOKEN|SECRET|APP_KEY)|password)\s*[:=]\s*"
    r"(?!System\.getenv|providers\.environmentVariable|\$\{|$)[^\s#]+"
)
VERSION_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]*$")
SHA_RE = re.compile(r"^[0-9a-f]{40}$")


class PublicSourceError(RuntimeError):
    pass


def read_json(path: Path) -> dict:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise PublicSourceError(f"Unable to read release metadata: {path}") from exc
    if not isinstance(value, dict):
        raise PublicSourceError(f"Release metadata must be a JSON object: {path}")
    return value


def scrub_allowed_legacy_text(relative: Path, content: str) -> str:
    for allowed_text in ALLOWED_LEGACY_TEXT.get(relative, ()):
        count = content.count(allowed_text)
        if count > 1:
            raise PublicSourceError(
                f"Public source repeats an approved compatibility literal: {relative}"
            )
        if count == 1:
            content = content.replace(allowed_text, "", 1)
    return content


def validate_required_compatibility_text(source: Path) -> None:
    for relative, required_texts in REQUIRED_COMPATIBILITY_TEXT.items():
        path = source / relative
        if not path.is_file():
            raise PublicSourceError(
                f"Public source is missing a required compatibility file: {relative}"
            )
        try:
            content = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError) as exc:
            raise PublicSourceError(
                f"Unable to read a required compatibility file: {relative}"
            ) from exc
        for required_text in required_texts:
            if content.count(required_text) != 1:
                raise PublicSourceError(
                    "Public source must contain an approved compatibility literal exactly once: "
                    f"{relative}"
                )


def validate_public_path(relative: Path) -> None:
    for part in relative.parts:
        lowered = part.lower()
        if any(identity_part in lowered for identity_part in FORMER_DEMO_PARTS):
            raise PublicSourceError(f"Public source contains a legacy path name: {relative}")


def validate_public_text(relative: Path, content: str) -> None:
    if PRIVATE_KEY_TEXT.search(content) or SECRET_ASSIGNMENT.search(content):
        raise PublicSourceError(f"Public source contains prohibited content: {relative}")

    scrubbed = scrub_allowed_legacy_text(relative, content)
    if any(pattern.search(scrubbed) for pattern in LEGACY_IDENTITY_TEXT):
        raise PublicSourceError(f"Public source contains legacy identity text: {relative}")
    if relative not in LOCALIZED_HAN_FILES and HAN_TEXT.search(content):
        raise PublicSourceError(f"Public source contains non-localized Han text: {relative}")


def source_digest(source: Path) -> str:
    digest = hashlib.sha256()
    for path in sorted(
        path
        for path in source.rglob("*")
        if path.is_file()
        and path.name != "RELEASE_METADATA.json"
        and path.relative_to(source).parts[0] != ".git"
    ):
        relative = path.relative_to(source).as_posix()
        digest.update(relative.encode("utf-8"))
        digest.update(b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\0")
    return digest.hexdigest()


def validate_source(
    source: Path,
    version: str,
    upstream_job: str,
    upstream_build: str,
    release_type: str,
    allow_root_git: bool = False,
    verify_release_integrity: bool = True,
) -> dict:
    if not source.is_dir():
        raise PublicSourceError(f"Public source directory does not exist: {source}")
    for relative in REQUIRED_FILES:
        if not (source / relative).is_file():
            raise PublicSourceError(f"Public source is missing a required file: {relative}")
    license_text = (source / "LICENSE").read_text(encoding="utf-8", errors="replace")
    if "Apache License" not in license_text or "Version 2.0, January 2004" not in license_text:
        raise PublicSourceError("LICENSE is not the standard Apache License 2.0 text")
    if not (source / "NOTICE").read_text(encoding="utf-8", errors="replace").strip():
        raise PublicSourceError("NOTICE must not be empty")
    if not (source / "nexconn-chatui" / "src").is_dir():
        raise PublicSourceError("Public source is missing nexconn-chatui/src")
    validate_required_compatibility_text(source)

    metadata = read_json(source / "RELEASE_METADATA.json")
    if verify_release_integrity:
        if not VERSION_RE.fullmatch(version):
            raise PublicSourceError("Invalid version format")
        try:
            build_number = int(upstream_build)
        except ValueError as exc:
            raise PublicSourceError("Upstream build must be a positive integer") from exc
        if build_number <= 0:
            raise PublicSourceError("Upstream build must be a positive integer")

        expected = {
            "license": "Apache-2.0",
            "chatui_version": version,
            "upstream_job": upstream_job,
            "upstream_build": build_number,
            "release_type": release_type,
        }
        for key, expected_value in expected.items():
            if metadata.get(key) != expected_value:
                raise PublicSourceError(f"RELEASE_METADATA.json field mismatch: {key}")
        if not metadata.get("chat_sdk_version"):
            raise PublicSourceError("RELEASE_METADATA.json is missing chat_sdk_version")
        if not SHA_RE.fullmatch(str(metadata.get("source_commit", ""))) or not SHA_RE.fullmatch(
            str(metadata.get("imkit_baseline", ""))
        ):
            raise PublicSourceError(
                "source_commit and imkit_baseline must be 40-character lowercase SHAs"
            )
        source_commit = (source / "SOURCE_COMMIT").read_text(encoding="utf-8").strip()
        if source_commit != metadata["source_commit"]:
            raise PublicSourceError("SOURCE_COMMIT does not match RELEASE_METADATA.json")

    for path in source.rglob("*"):
        relative = path.relative_to(source)
        if allow_root_git and relative.parts[0] == ".git":
            continue
        validate_public_path(relative)
        relative_parts = {part.lower() for part in relative.parts}
        if relative_parts & FORBIDDEN_NAMES or path.is_symlink():
            raise PublicSourceError(f"Public source contains a prohibited entry: {relative}")
        if path.is_file():
            if (
                path.name.lower() in FORBIDDEN_CREDENTIAL_NAMES
                or path.suffix.lower() in FORBIDDEN_CREDENTIAL_SUFFIXES
            ):
                raise PublicSourceError(f"Public source contains a credential file: {relative}")
            try:
                content = path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue
            validate_public_text(relative, content)

    if verify_release_integrity:
        calculated_digest = source_digest(source)
        if not metadata.get("source_digest"):
            raise PublicSourceError("RELEASE_METADATA.json is missing source_digest")
        if metadata["source_digest"] != calculated_digest:
            raise PublicSourceError(
                "RELEASE_METADATA.json source_digest does not match the source tree"
            )
    return metadata


def parse_args(argv: List[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default=".")
    parser.add_argument("--version")
    parser.add_argument("--upstream-job")
    parser.add_argument("--upstream-build")
    parser.add_argument("--release-type")
    parser.add_argument("--allow-root-git", action="store_true")
    parser.add_argument(
        "--contribution",
        action="store_true",
        help=(
            "keep public safety checks but skip source provenance and digest checks "
            "that only apply to release snapshots"
        ),
    )
    return parser.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv if argv is not None else sys.argv[1:])
    source = Path(args.source).resolve()
    try:
        metadata = read_json(source / "RELEASE_METADATA.json")
        validate_source(
            source,
            args.version or str(metadata.get("chatui_version", "")),
            args.upstream_job or str(metadata.get("upstream_job", "")),
            args.upstream_build or str(metadata.get("upstream_build", "")),
            args.release_type or str(metadata.get("release_type", "")),
            allow_root_git=args.allow_root_git,
            verify_release_integrity=not args.contribution,
        )
        print(f"public source validation passed: {source}")
        return 0
    except (PublicSourceError, OSError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
