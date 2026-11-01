"""Voix off (edge-tts fr-FR-HenriNeural), normalisée à -16 LUFS, avec horodatage des mots.
Prononciation demandée par Hillel : « Hitbodédoute » (écrit ainsi pour la synthèse ; sous-titres : Hitbodedut).
Usage : python3 voix.py s03 s05 ...  (sans argument : toutes)"""
import asyncio, ssl, json, sys, subprocess, os, edge_tts, edge_tts.communicate as Cm
Cm._SSL_CTX = ssl.create_default_context(cafile="/root/.ccr/ca-bundle.crt")
DOS = os.path.join(os.path.dirname(__file__), '..', 'remotion', 'public', 'voix')
MOTS = os.path.join(os.path.dirname(__file__), 'mots_voix.json')
H = "Hitbodédoute"
TEXTES = {
 's03': f"La {H}, c'est s'arrêter… et prendre un moment rien que pour Hachem.",
 's04a': "Lui parler avec tes propres mots. Lui raconter tes difficultés, tes rêves, tes joies…",
 's04b': "et tes remerciements.",
 's05': f"Pour t'accompagner dans cette démarche, découvre My {H}.",
 's06': f"Cinq étapes pour t'aider à structurer ta {H}, inspirées des enseignements de nos Sages.",
}
async def une(k, t):
    c = edge_tts.Communicate(t, "fr-FR-HenriNeural", rate="-8%", pitch="-4Hz", boundary="WordBoundary")
    ws, mp3 = [], f"/tmp/{k}.mp3"
    with open(mp3, 'wb') as f:
        async for x in c.stream():
            if x["type"] == "audio": f.write(x["data"])
            elif x["type"] == "WordBoundary": ws.append((round(x["offset"] / 1e7, 2), x["text"]))
    return ws, mp3
async def main(cles):
    mots = json.load(open(MOTS))
    for k in cles:
        ws, mp3 = await une(k, TEXTES[k])
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', mp3, '-ar', '48000', '-ac', '1', f"/tmp/{k}.wav"], check=True)
        mots[k] = ws; print(k, ws)
    json.dump(mots, open(MOTS, 'w'), ensure_ascii=False, indent=0)
asyncio.run(main(sys.argv[1:] or list(TEXTES)))
