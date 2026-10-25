#!/usr/bin/env python3
"""Makes assets/audio/*.ogg from 'name<TAB>phonemes<TAB>speed' lines on stdin (eSpeak NG, Icelandic phoneme table)."""
import os, subprocess, sys, tempfile

out = os.path.join(os.path.dirname(__file__), '..', 'assets', 'audio')
os.makedirs(out, exist_ok=True)
for line in sys.stdin:
    name, pron, speed = line.rstrip('\n').split('\t')
    ph = pron.replace('r', '_|R')        # rolled r
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        wav = tmp.name
    subprocess.run(['espeak-ng', '-v', 'is', '-s', speed, '-w', wav, '[[' + ph + ']]'], check=True)
    subprocess.run(['ffmpeg', '-nostdin', '-y', '-loglevel', 'error', '-i', wav,
                    '-af', 'adelay=80,apad=pad_dur=0.15,loudnorm=I=-16:TP=-1.5,aresample=22050',
                    '-ac', '1', '-c:a', 'libvorbis', '-q:a', '3', os.path.join(out, name + '.ogg')], check=True)
    os.remove(wav)
print('done', len(os.listdir(out)), 'files')
