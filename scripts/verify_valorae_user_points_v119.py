#!/usr/bin/env python3
"""Compat wrapper: user UI points were superseded by the v2.0.3 audit."""
import runpy
from pathlib import Path
root = Path(__file__).resolve().parents[1]
runpy.run_path(str(root / 'scripts' / 'verify_valorae_ui_v203.py'), run_name='__main__')
