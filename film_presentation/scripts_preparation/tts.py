import sys,ssl,asyncio,edge_tts.communicate as c
c._SSL_CTX=ssl.create_default_context(cafile='/root/.ccr/ca-bundle.crt')
voice,rate,text,out=sys.argv[1:5]
asyncio.run(c.Communicate(text,voice,rate=rate).save(out))
