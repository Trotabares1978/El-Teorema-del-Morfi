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
