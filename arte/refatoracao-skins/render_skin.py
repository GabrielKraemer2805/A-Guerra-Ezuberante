"""Renderizador ortografico por raios que le cubos e UVs reais de geo/humanoid.geo.json.

Nao ha mockup: cada pixel da imagem vem do texel que o UV do modelo do mod aponta.
Convencao do modelo: frente em z minimo, braco direito em x negativo, 'east' = lado direito do personagem.
"""
import json, math
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
GEO = ROOT / "src/main/resources/assets/guerra_ezuberante/geo/humanoid.geo.json"

def load_cubes():
    geo = json.loads(GEO.read_text(encoding="utf-8"))["minecraft:geometry"][0]
    cubes = []
    for bone in geo["bones"]:
        for c in bone.get("cubes", []):
            inf = c.get("inflate", 0)
            o, s = c["origin"], c["size"]
            lo = [o[i] - inf for i in range(3)]
            hi = [o[i] + s[i] + inf for i in range(3)]
            cubes.append((lo, hi, c["uv"], bone["name"]))
    return cubes

CUBES = load_cubes()
SHADE = {"up": 1.0, "north": .9, "south": .78, "west": .84, "east": .7, "down": .5}

def _texel(tex, face, uv, lo, hi, p):
    u0, v0 = uv[face]["uv"]; us, vs = uv[face]["uv_size"]
    w, h, d = hi[0]-lo[0], hi[1]-lo[1], hi[2]-lo[2]
    x, y, z = p
    if face == "north": a, b = (x-lo[0])/w, (hi[1]-y)/h
    elif face == "south": a, b = (hi[0]-x)/w, (hi[1]-y)/h
    elif face == "east": a, b = (hi[2]-z)/d, (hi[1]-y)/h
    elif face == "west": a, b = (z-lo[2])/d, (hi[1]-y)/h
    elif face == "up": a, b = (x-lo[0])/w, (hi[2]-z)/d
    else: a, b = (x-lo[0])/w, (z-lo[2])/d
    a, b = min(max(a, 0), .999999), min(max(b, 0), .999999)
    tu = u0 + a*us; tv = v0 + b*vs
    if vs < 0: tv -= 1e-6
    return tex[int(tu), int(tv)]

def render(skin, yaw=0.0, pitch=0.0, scale=10, shade=False, bg=(0, 0, 0, 0), only=None):
    """yaw 0 = frente; 180 = costas; 90 = lado direito do personagem; -90 = lado esquerdo."""
    img = Image.open(skin).convert("RGBA") if not isinstance(skin, Image.Image) else skin.convert("RGBA")
    tex = img.load()
    t, p = math.radians(yaw), math.radians(pitch)
    fx, fz = math.sin(t), math.cos(t)
    r = (math.cos(t), 0.0, -math.sin(t))
    f = (fx*math.cos(p), -math.sin(p), fz*math.cos(p))
    u = (fx*math.sin(p), math.cos(p), fz*math.sin(p))
    cubes = [c for c in CUBES if only is None or c[3] in only]
    W, H = (14 if only else 24), (12 if only else 36)
    cy = 28 if only else 16
    out = Image.new("RGBA", (W*scale, H*scale), bg)
    px = out.load()
    for sy in range(H*scale):
        for sx in range(W*scale):
            a = (sx+.5)/scale - W/2; b = H/2 - (sy+.5)/scale
            o = [r[i]*a + u[i]*b - f[i]*60 for i in range(3)]
            o[1] += cy
            hits = []
            for lo, hi, uv, _ in cubes:
                tn, tx, axis = -1e9, 1e9, 0
                ok = True
                for i in range(3):
                    if abs(f[i]) < 1e-9:
                        if o[i] < lo[i] or o[i] > hi[i]: ok = False; break
                    else:
                        t1, t2 = (lo[i]-o[i])/f[i], (hi[i]-o[i])/f[i]
                        if t1 > t2: t1, t2 = t2, t1
                        if t1 > tn: tn, axis = t1, i
                        if t2 < tx: tx = t2
                        if tn > tx: ok = False; break
                if ok: hits.append((tn, axis, lo, hi, uv))
            hits.sort(key=lambda h: h[0])
            for tn, axis, lo, hi, uv in hits:
                q = [o[i]+f[i]*tn for i in range(3)]
                if axis == 0: face = "east" if f[0] > 0 else "west"
                elif axis == 1: face = "up" if f[1] < 0 else "down"
                else: face = "north" if f[2] > 0 else "south"
                c = _texel(tex, face, uv, lo, hi, q)
                if c[3] == 0: continue
                if shade:
                    m = SHADE[face]; c = (int(c[0]*m), int(c[1]*m), int(c[2]*m), 255)
                px[sx, sy] = c
                break
    return out
