#!/usr/bin/env python3
"""Mamorona ny icon pro (boky misokatra + hazavana + hazofijoro) ho an'ny LKL.
Fampiasana: python3 generate_icons.py  (avy ao amin'ny lahatahiry fototra)"""
from PIL import Image, ImageDraw, ImageFilter
import os, math

S = 1024
def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))

def bez(p0, p1, p2, p3, n=40):
    pts = []
    for i in range(n + 1):
        t = i / n; u = 1 - t
        x = u**3*p0[0] + 3*u*u*t*p1[0] + 3*u*t*t*p2[0] + t**3*p3[0]
        y = u**3*p0[1] + 3*u*u*t*p1[1] + 3*u*t*t*p2[1] + t**3*p3[1]
        pts.append((x, y))
    return pts

def gradient_bg(size):
    top, bot = (24, 30, 58), (9, 11, 22)
    img = Image.new("RGB", (size, size))
    px = img.load()
    for y in range(size):
        c = lerp(top, bot, y / (size - 1))
        for x in range(size): px[x, y] = c
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cx, cy, r = size // 2, int(size * 0.40), int(size * 0.46)
    gd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(212, 160, 23, 70))
    glow = glow.filter(ImageFilter.GaussianBlur(size * 0.10))
    img = img.convert("RGBA"); img.alpha_composite(glow)
    return img

def gold_fill(size, poly, c1, c2):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    grad = Image.new("RGBA", (size, size))
    gp = grad.load()
    ys = [p[1] for p in poly]; y0, y1 = min(ys), max(ys)
    for y in range(size):
        t = min(1, max(0, (y - y0) / max(1, y1 - y0)))
        c = lerp(c1, c2, t) + (255,)
        for x in range(size): gp[x, y] = c
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).polygon(poly, fill=255)
    layer.paste(grad, (0, 0), mask)
    return layer

def foreground():
    """Calque transparent : boky + hazavana + hazofijoro (zone sûre ~ 66%)."""
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sp = (512, 640)
    # rayons
    rays = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rays)
    for ang in range(-60, 61, 20):
        a = math.radians(ang - 90)
        a1, a2 = a - 0.045, a + 0.045
        ox, oy = 512, 400
        pts = [(ox, oy), (ox + 330*math.cos(a1), oy + 330*math.sin(a1)),
               (ox + 330*math.cos(a2), oy + 330*math.sin(a2))]
        rd.polygon(pts, fill=(255, 214, 102, 150))
    rays = rays.filter(ImageFilter.GaussianBlur(5))
    img.alpha_composite(rays)
    # base/cover du livre
    cover_l = [(512, 700)] + bez((512, 700), (400, 725), (300, 715), (232, 690)) + [(232, 560), (512, 585)]
    cover_r = [(512, 700)] + bez((512, 700), (624, 725), (724, 715), (792, 690)) + [(792, 560), (512, 585)]
    cd = ImageDraw.Draw(img)
    cd.polygon(cover_l, fill=(120, 82, 12)); cd.polygon(cover_r, fill=(120, 82, 12))
    # pages gauche / droite
    left = bez((512, 500), (440, 455), (330, 450), (250, 480)) + [(250, 665)] + \
           bez((250, 665), (330, 640), (430, 650), (512, 690))
    right = [(2*512 - x, y) for (x, y) in left]
    img.alpha_composite(gold_fill(S, left, (255, 226, 130), (214, 160, 36)))
    img.alpha_composite(gold_fill(S, right, (255, 236, 160), (226, 172, 44)))
    d = ImageDraw.Draw(img)
    # reliure
    d.line([(512, 500), (512, 690)], fill=(120, 82, 12), width=8)
    # lignes de texte
    for i in range(4):
        y = 540 + i * 30
        d.line(bez((290, y + 8), (350, y - 8), (430, y - 4), (490, y + 18), 20), fill=(176, 126, 24), width=6)
        d.line([(2*512 - x, yy) for (x, yy) in bez((290, y + 8), (350, y - 8), (430, y - 4), (490, y + 18), 20)],
               fill=(176, 126, 24), width=6)
    # hazofijoro
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([512-110, 330-110, 512+110, 330+110], fill=(255, 220, 110, 120))
    glow = glow.filter(ImageFilter.GaussianBlur(28)); img.alpha_composite(glow)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([488, 250, 536, 410], radius=10, fill=(255, 244, 205))
    d.rounded_rectangle([450, 296, 574, 340], radius=10, fill=(255, 244, 205))
    return img

def rounded(img, ratio):
    s = img.size[0]
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, s - 1, s - 1], radius=int(s * ratio), fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0)); out.paste(img, (0, 0), m); return out

bg = gradient_bg(S)
fg0 = foreground()
fg = Image.new('RGBA',(S,S),(0,0,0,0)); fg.paste(fg0,(0,105),fg0)
full = bg.copy(); full.alpha_composite(fg)

res = "app/src/main/res"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for dens, sz in SIZES.items():
    os.makedirs(f"{res}/mipmap-{dens}", exist_ok=True)
    rounded(full, 0.22).resize((sz, sz), Image.LANCZOS).save(f"{res}/mipmap-{dens}/ic_launcher.png")
    rounded(full, 0.5).resize((sz, sz), Image.LANCZOS).save(f"{res}/mipmap-{dens}/ic_launcher_round.png")

# icône adaptative (API 26+)
os.makedirs(f"{res}/drawable-nodpi", exist_ok=True)
os.makedirs(f"{res}/mipmap-anydpi-v26", exist_ok=True)
bg.convert("RGB").resize((432, 432), Image.LANCZOS).save(f"{res}/drawable-nodpi/ic_launcher_background.png")
# le calque avant est rétréci pour rester dans la zone sûre (66/108)
fg_small = fg.resize((432, 432), Image.LANCZOS)
fg_small.save(f"{res}/drawable-nodpi/ic_launcher_foreground.png")
xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
'''
for n in ("ic_launcher", "ic_launcher_round"):
    open(f"{res}/mipmap-anydpi-v26/{n}.xml", "w").write(xml)
full.resize((512, 512), Image.LANCZOS).save("icon_preview.png")
print("OK")
