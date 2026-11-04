import sys
from rembg import remove, new_session
from PIL import Image
s=new_session('birefnet-general')
n,f=sys.argv[1],sys.argv[2]
remove(Image.open(f).convert('RGB'),session=s,only_mask=True).save(f'mask_{n}.png')
