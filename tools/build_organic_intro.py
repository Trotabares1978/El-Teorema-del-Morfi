from pathlib import Path
import base64,re,json,io,cv2,numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
html=(ROOT/"index.html").read_text(encoding="utf-8")
m=re.search(r"const I=(\[.*?\]),B=(\[.*?\])\s*const bg=",html,re.S)
if not m: raise SystemExit("No se encontraron las imágenes embebidas")
def arr(s): return "".join(json.loads(s)).replace("\\n","").replace(" ","")

def dec(s): return base64.b64decode(s + "="*((4-len(s)%4)%4))
cover=Image.open(io.BytesIO(dec(arr(m.group(1))))).convert("RGB")
bg=Image.open(io.BytesIO(dec(arr(m.group(2))))).convert("RGB").resize(cover.size,Image.Resampling.LANCZOS)
a=np.asarray(cover).astype(np.int16); b=np.asarray(bg).astype(np.int16)
diff=np.max(np.abs(a-b),axis=2).astype(np.uint8)
diff=cv2.GaussianBlur(diff,(0,0),1.1)
fg=(diff>18).astype(np.uint8)*255
fg=cv2.morphologyEx(fg,cv2.MORPH_CLOSE,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(5,5)))
fg=cv2.morphologyEx(fg,cv2.MORPH_OPEN,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(3,3)))
H,W=fg.shape; Y,X=np.mgrid[0:H,0:W]
def roi(x0,y0,x1,y1,soft=1.0):
    z=np.zeros_like(fg); z[y0:y1,x0:x1]=fg[y0:y1,x0:x1]
    return cv2.GaussianBlur(z,(0,0),soft)
out=ROOT/"app/src/main/assets/morfi_intro_layers"; out.mkdir(parents=True,exist_ok=True)
def save(name,mask):
    rgba=np.dstack([a.astype(np.uint8),np.clip(mask,0,255).astype(np.uint8)])
    Image.fromarray(rgba,"RGBA").save(out/f"{name}.png",optimize=True)
road=roi(55,300,457,768,1.2); road[(Y>425)&(X>165)&(X<365)]=0; road[(Y>690)&(X>100)&(X<412)]=0; save("path",road)
for name,box in [("astillero",(0,245,190,385)),("pizzeria",(35,295,195,425)),("escuela",(315,245,512,390)),("clinica",(255,300,410,435))]: save(name,roi(*box,.9))
save("title",roi(55,0,457,205,1.0))
mat=roi(135,390,390,745,1.15); mat[:430,:]=0; save("matias",mat)
save("enter",roi(105,675,415,768,1.1))
bg.save(out/"background.png",optimize=True)

# organic intro build trigger

html_out = r'''<!doctype html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<title>El Teorema del Morfi</title>
<style>
*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#f7f1e5}
.scene{position:absolute;inset:0;display:flex;align-items:center;justify-content:center}
.art{position:relative;width:min(100vw,66.6667vh);height:min(150vh,100vh);overflow:hidden;background:#f7f1e5}
.layer{position:absolute;inset:0;width:100%;height:100%;object-fit:fill;opacity:0}
@keyframes fade{from{opacity:0}to{opacity:1}}
@keyframes softMat{from{opacity:0;transform:translateY(12px) scale(.985);filter:blur(4px)}to{opacity:1;transform:none;filter:none}}
.path{animation:fade 2.4s .2s ease-out forwards}
.astillero{animation:fade 1.1s 2.1s ease-out forwards}
.pizzeria{animation:fade 1.1s 2.45s ease-out forwards}
.escuela{animation:fade 1.1s 2.8s ease-out forwards}
.clinica{animation:fade 1.1s 3.15s ease-out forwards}
.title{animation:fade 1s 3.8s ease-out forwards}
.matias{animation:softMat 1.5s 4.65s ease-out forwards}
.enter{animation:fade 1s 6s ease-out forwards}
.enter{cursor:pointer}
.soundHint{position:absolute;z-index:20;left:50%;bottom:18px;transform:translateX(-50%);padding:9px 14px;border:1px solid #fff8;border-radius:12px;background:#fff9;color:#3d2d22;font:700 13px Georgia,serif}
.off{display:none}
</style></head><body>
<div class="scene"><div class="art" id="art">
<img class="layer" src="./app/src/main/assets/morfi_intro_layers/background.png">
<img class="layer path" src="./app/src/main/assets/morfi_intro_layers/path.png">
<img class="layer astillero" src="./app/src/main/assets/morfi_intro_layers/astillero.png">
<img class="layer pizzeria" src="./app/src/main/assets/morfi_intro_layers/pizzeria.png">
<img class="layer escuela" src="./app/src/main/assets/morfi_intro_layers/escuela.png">
<img class="layer clinica" src="./app/src/main/assets/morfi_intro_layers/clinica.png">
<img class="layer title" src="./app/src/main/assets/morfi_intro_layers/title.png">
<img class="layer matias" src="./app/src/main/assets/morfi_intro_layers/matias.png">
<img class="layer enter" id="enter" src="./app/src/main/assets/morfi_intro_layers/enter.png">
</div></div>
<button class="soundHint" id="hint">Tocá una vez para activar la música</button>
<audio id="music" loop src="./app/src/main/res/raw/el_teorema_del_morfi.mp3"></audio>
<script>
const music=document.getElementById('music'),hint=document.getElementById('hint'),enter=document.getElementById('enter');
let started=false;
function sound(){if(started)return;music.volume=.55;music.play().then(()=>{started=true;hint.classList.add('off')}).catch(()=>{});}
window.addEventListener('load',()=>{music.volume=.55;music.play().then(()=>{started=true;hint.classList.add('off')}).catch(()=>{});});
document.addEventListener('pointerdown',sound,{once:true});
document.addEventListener('touchstart',sound,{once:true,passive:true});
enter.addEventListener('click',()=>{document.getElementById('art').style.transition='opacity .6s';document.getElementById('art').style.opacity='0';setTimeout(()=>location.href='menu.html',620);});
</script></body></html>'''
(ROOT/"index.html").write_text(html_out,encoding="utf-8")
