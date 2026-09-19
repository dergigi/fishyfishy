#!/usr/bin/env python3
"""Publish the signed GitHub release using FishyFishy's checked-in metadata.

SIGN_WITH can be in the environment or a gitignored .env file. A different
file can be supplied with --env-file, e.g. an existing publisher's configuration.
Secrets are passed only in the subprocess environment, never command arguments.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--env-file', type=Path, default=root / '.env')
parser.add_argument('--link-identity', action='store_true', help='Link the configured Android key to the publisher before publishing')
args = parser.parse_args()
env = os.environ.copy()
env['PATH'] = str(Path.home() / 'bin') + os.pathsep + env.get('PATH', '')
if not env.get('SIGN_WITH') and args.env_file.exists():
    for line in args.env_file.read_text().splitlines():
        if line.startswith('SIGN_WITH='):
            env['SIGN_WITH'] = line.split('=', 1)[1].strip().strip("\"'")
if not env.get('SIGN_WITH'):
    raise SystemExit('Set SIGN_WITH to your publishing identity, or provide --env-file.')
if not shutil.which('zsp', path=env['PATH']):
    raise SystemExit('Install zsp from https://github.com/zapstore/zsp/releases.')
if not env.get('GITHUB_TOKEN') and shutil.which('gh'):
    token = subprocess.run(['gh', 'auth', 'token'], capture_output=True, text=True)
    if token.returncode == 0:
        env['GITHUB_TOKEN'] = token.stdout.strip()
if args.link_identity:
    props = {}
    local = root / 'local.properties'
    if local.exists():
        for line in local.read_text().splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                key, value = line.split('=', 1)
                props[key.strip()] = value.strip()
    def setting(key):
        value = env.get(key) or props.get(key)
        if not value:
            raise SystemExit(f'Missing {key} for certificate linking.')
        return value
    key_file = Path(setting('OEM_STORE_FILE'))
    if not key_file.is_absolute():
        key_file = (root / key_file).resolve()
    env['KEYSTORE_PASSWORD'] = setting('OEM_STORE_PASSWORD')
    # Java often stores PKCS12 data in a file named .jks; zsp uses the suffix.
    with key_file.open('rb') as source:
        suffix = '.jks' if source.read(4) == bytes.fromhex('feedfeed') else '.p12'
    with tempfile.TemporaryDirectory(prefix='fishyfishy-signing-') as directory:
        link = Path(directory) / ('signing' + suffix)
        link.symlink_to(key_file)
        subprocess.run(['zsp', 'identity', '--link-key', str(link), '--key-alias', setting('OEM_KEY_ALIAS'),
                        '--relays', 'wss://relay.zapstore.dev'], env=env, cwd=root, check=True)

# Verify the signing key's publisher link before publishing.
apk = root / 'app/build/outputs/apk/release/app-release.apk'
if not apk.exists():
    raise SystemExit('Build :app:assembleRelease before publishing.')
publisher = next(line.split(':', 1)[1].strip() for line in (root / 'zapstore.yaml').read_text().splitlines() if line.startswith('pubkey:'))
subprocess.run(['zsp', 'identity', '--verify', str(apk), '--relays', 'wss://relay.zapstore.dev'], input=publisher + '\n', text=True, env=env, cwd=root, check=True)
subprocess.run(['zsp', 'publish', 'zapstore.yaml', '--quiet', '--skip-preview', '--skip-certificate-linking'], env=env, cwd=root, check=True)
