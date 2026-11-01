"""Voix off v16 : voix plus douce (edge-tts fr-FR-RemyMultilingualNeural, -10 %), aigus adoucis, -16 LUFS.
Prononciation demandée par Hillel : « Hitbodédoute » (écrit ainsi pour la synthèse ; sous-titres : Hitbodedut).
Usage : python3 voix.py [clés...]  (sans argument : toutes). Écrit public/voix/<clé>.wav et mots_voix.json."""
import asyncio, ssl, json, sys, subprocess, os, edge_tts, edge_tts.communicate as Cm
Cm._SSL_CTX = ssl.create_default_context(cafile="/root/.ccr/ca-bundle.crt")
ICI = os.path.dirname(os.path.abspath(__file__))
DOS = os.path.join(ICI, '..', 'remotion', 'public', 'voix'); MOTS = os.path.join(ICI, 'mots_voix.json')
VOIX, DEBIT, HAUTEUR = "fr-FR-RemyMultilingualNeural", "-10%", "-2Hz"
H = "Hitbodédoute"
TEXTES = {
 's01': "Chaque jour, nous sommes occupés. Le travail, les responsabilités, les soucis…",
 's02': "Mais prenons-nous vraiment le temps de parler à Hachem ?",
 's05a': f"My {H}.",
 's05b': "Pour t'accompagner dans cette démarche.",
 's03': f"La {H}, c'est s'arrêter… et prendre un moment rien que pour Hachem.",
 's04a': "Lui parler avec tes propres mots. Lui raconter tes difficultés, tes rêves, tes joies…",
 's04b': "et tes remerciements.",
 's06': f"Cinq étapes pour t'aider à structurer ta {H}, inspirées des enseignements de nos Sages.",
 's07': "Choisis la durée de ton rendez-vous quotidien, et avance à ton rythme.",
 's08': "Bloque les distractions. Mets ton téléphone de côté. Ce moment est consacré à Hachem.",
 's09': "Personnalise ton expérience selon tes préférences et ton rythme de vie.",
 's10c': "Avec le calendrier juif, retrouve chaque jour la date hébraïque, les zmanim et les ajouts de la prière.",
 's11': "Ne laisse pas passer une journée sans prendre un moment pour Hachem. Commence aujourd'hui.",
}
async def une(k, t):
    c = edge_tts.Communicate(t, VOIX, rate=DEBIT, pitch=HAUTEUR, boundary="WordBoundary")
    ws, mp3 = [], f"/tmp/{k}.mp3"
    with open(mp3, 'wb') as f:
        async for x in c.stream():
            if x["type"] == "audio": f.write(x["data"])
            elif x["type"] == "WordBoundary": ws.append((round(x["offset"] / 1e7, 2), x["text"]))
    return ws, mp3
async def main(cles):
    mots = json.load(open(MOTS)) if os.path.exists(MOTS) else {}
    for k in cles:
        ws, mp3 = await une(k, TEXTES[k])
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', mp3, '-ar', '48000', '-ac', '1', f"/tmp/{k}.wav"], check=True)
        mots['v16_' + k] = ws
    json.dump(mots, open(MOTS, 'w'), ensure_ascii=False, indent=0)
asyncio.run(main(sys.argv[1:] or list(TEXTES)))
