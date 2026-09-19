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

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--env-file', type=Path, default=root / '.env')
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
# The existing signing key is already linked to this publisher (as used by Boris).
# Verify that link before publishing; never silently skip a failed identity check.
apk = root / 'app/build/outputs/apk/release/app-release.apk'
if not apk.exists():
    raise SystemExit('Build :app:assembleRelease before publishing.')
publisher = next(line.split(':', 1)[1].strip() for line in (root / 'zapstore.yaml').read_text().splitlines() if line.startswith('pubkey:'))
subprocess.run(['zsp', 'identity', '--verify', str(apk)], input=publisher + '\n', text=True, env=env, cwd=root, check=True)
subprocess.run(['zsp', 'publish', 'zapstore.yaml', '--quiet', '--skip-preview', '--skip-certificate-linking'], env=env, cwd=root, check=True)
