#!/usr/bin/env python3
"""Fetch redistributable reference photos with their Commons attribution records."""
import argparse, concurrent.futures, html, json, pathlib, re, urllib.parse, urllib.request
ROOT = pathlib.Path(__file__).resolve().parents[1]
SPECIES = {'wrasse':'Ornate wrasse','salema':'Salema porgy','parrotfish':'Sparisoma cretense','bream':'Diplodus sargus','zebra':'Diplodus cervinus','damselfish':'Similiparma lurida','comber':'Serranus atricauda','grouper':'Dusky grouper','octopus':'Octopus vulgaris','cuttlefish':'Common cuttlefish','urchin':'Diadema africanum','starfish':'Ophidiaster ophidianus'}
CURATED = {
    'limpet': 'Patella aspera 01.JPG',
    'hermit_crab': 'Calcinus tubularis 1.jpg',
    'saddled': 'Oblada (Oblada melanura), Ċirkewwa, Malta, Malta, 2021-08-24, DD 26.jpg',
    'two_banded': 'Sparidae - Diplodus vulgaris.JPG',
    'mullet': 'Liza aurata Corsica.jpg',
    'goatfish': 'Salmonete de roca (Mullus surmuletus), Madeira, Portugal, 2019-05-31, DD 55.jpg',
    'lizardfish': 'Pez lagarto rayado (Synodus saurus), franja marina Teno-Rasca, Tenerife, España, 2022-01-08, DD 51.jpg',
    'triplefin': 'Moma amarilla (Tripterygion delaisi), Parque natural de la Arrábida, Portugal, 2022-07-20, DD 29.jpg',
    'redlip': 'Blénidos (Ophioblennius atlanticus), franja marina Teno-Rasca, Tenerife, España, 2022-01-09, DD 29.jpg',
    'toby': 'Canthigaster capistrata.jpg',
    'puffer': 'Pez globo ajedrez (Sphoeroides marmoratus), franja marina Teno-Rasca, Tenerife, España, 2022-01-05, DD 68.jpg',
    'scorpionfish': 'Scorpaena maderensis.jpg',
    'black_moray': 'Morenas negras (Muraena augusti), franja marina Teno-Rasca, Tenerife, España, 2022-01-09, DD 52.jpg',
    'moray': 'Morena del Mediterráneo (Muraena helena), Regga, Gozo, Malta, 2021-08-23, DD 14.jpg',
    'trumpetfish': 'Pez trompeta atlántico (Aulostomus strigosus), Madeira, Portugal, 2019-05-31, DD 23.jpg',
    'triggerfish': 'Pez ballesta (Balistes capriscus), Parque natural de la Arrábida, Portugal, 2021-09-10, DD 11.jpg',
    'barracuda': 'Bicuda (Sphyraena viridensis), franja marina Teno-Rasca, Tenerife, España, 2022-01-08, DD 40.jpg',
    'sand_bream': 'Sand steenbras.jpg',
    'fireworm': 'Gusano de fuego (Hermodice carunculata), Madeira, Portugal, 2019-05-31, DD 45.jpg',
    'rock_crab': 'Grapsus adscensionis - Tenerife 01.jpg',
    'nimble_crab': 'Percnon gibbesi Linosa 053.jpg',
    'arrow_crab': 'Stenorhynchus lanceolatus.jpg',
    'cleaner_shrimp': 'Lysmata grabhami1.jpg',
    'black_urchin': 'Arbacia lixula (oursin noir).JPG',
    'purple_urchin': 'Paracentrotus lividus profil.JPG',
    'sea_cucumber': 'Holothuria sanctori, Balie, Crete, Greece - 20070601.jpg',
    'club_anemone': 'Anémona (Telmatactis cricoides), franja marina Teno-Rasca, Tenerife, España, 2022-01-05, DD 117.jpg',
    'snakelocks': 'Anémona de mar común (Anemonia viridis), Parque natural de la Arrábida, Portugal, 2020-07-21, DD 07.jpg',
    'sponge': 'Esponja de tubo amarilla (Aplysina aerophoba), Madeira, Portugal, 2019-05-30, DD 10.jpg',
    'spiny_star': 'Marthasterias glacialis Linosa 066.jpg',

    'azores_chromis': 'Fula blanca (Chromis limbata), franja marina Teno-Rasca, Tenerife, España, 2022-01-09, DD 13.jpg',
    'bogue': 'Banco de bogas (Boops boops), franja marina Teno-Rasca, Tenerife, España, 2022-01-08, DD 43.jpg',
    'sand_smelt': 'Atherina presbyter.jpg',
    'sardine': 'Sardina pilchardus 2011.jpg',
    'chromis': 'Chromis chromis (Linnaeus, 1758) - juvénile.jpg',
    'chromis_adult': 'Chromis chromis.jpg',
    'damselfish_blue': 'Similiparma lurida.png',
}
SPECIES.update({key: key for key in CURATED})
def get(url):
    req=urllib.request.Request(url,headers={'User-Agent':'FishyFishy/0.1 (Madeira educational field guide; https://github.com/dergigi)'})
    return urllib.request.urlopen(req,timeout=60).read()
def fetch(pair):
    key,title=pair
    if key in CURATED:
        filename=CURATED[key]
        url='https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode({'action':'query','format':'json','redirects':1,'titles':'File:'+filename,'prop':'imageinfo','iiprop':'url|extmetadata','iiurlwidth':1000})
        info=next(iter(json.loads(get(url))['query']['pages'].values()))['imageinfo'][0]
    elif key in ('comber','urchin','starfish'):
        taxon={'comber':'Serranus atricauda','urchin':'Diadema africanum','starfish':'Ophidiaster ophidianus'}[key]
        query={'action':'query','format':'json','generator':'search','gsrsearch':taxon+' filetype:bitmap','gsrnamespace':6,'gsrlimit':8,'prop':'imageinfo','iiprop':'url|extmetadata','iiurlwidth':1000}
        pages=json.loads(get('https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode(query)))['query']['pages']
        candidates=[p for p in pages.values() if any(x in p.get('imageinfo',[{}])[0].get('extmetadata',{}).get('LicenseShortName',{}).get('value','').lower() for x in ['cc by','public domain','cc0'])]
        chosen=sorted(candidates,key=lambda p:p.get('index',100))[0]
        filename=chosen['title'].removeprefix('File:')
        info=chosen['imageinfo'][0]
    else:
        summary=json.loads(get('https://en.wikipedia.org/api/rest_v1/page/summary/'+urllib.parse.quote(title.replace(' ','_'))))
        original=summary['originalimage']['source'].split('?')[0]
        filename=urllib.parse.unquote(original.split('/')[-2 if '/thumb/' in original else -1])
        url='https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode({'action':'query','format':'json','redirects':1,'titles':'File:'+filename,'prop':'imageinfo','iiprop':'url|extmetadata','iiurlwidth':1000})
        pages=json.loads(get(url))['query']['pages']
        info=next(iter(pages.values()))['imageinfo'][0]
    meta=info['extmetadata']
    clean=lambda s:html.unescape(re.sub('<[^>]+>','',s))
    license=clean(meta.get('LicenseShortName',{}).get('value',''))
    if not any(x in license.lower() for x in ['cc by','public domain','cc0']): raise ValueError(f'{key}: unsupported license {license}')
    image_url=info.get('thumburl',info['url'])
    extension = '.png' if '.png' in urllib.parse.urlparse(image_url).path.lower() else '.jpg'
    (ROOT/'app/src/main/res/drawable-nodpi'/f'{key}{extension}').write_bytes(get(image_url))
    return {'id':key,'file':filename,'author':clean(meta.get('Artist',{}).get('value','')),'license':license,'licenseUrl':meta.get('LicenseUrl',{}).get('value',''),'source':info['descriptionurl'],'imageUrl':image_url,'changes':'Resized by Wikimedia; displayed cropped in the app.'}
parser=argparse.ArgumentParser()
parser.add_argument('--only', nargs='+', choices=SPECIES.keys())
args=parser.parse_args()
credit_path=ROOT/'app/src/main/assets/photo-credits.json'
records={r['id']:r for r in json.loads(credit_path.read_text())} if credit_path.exists() else {}
selected={k:v for k,v in SPECIES.items() if not args.only or k in args.only}
failed=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    futures={pool.submit(fetch,p):p for p in selected.items()}
    for future in concurrent.futures.as_completed(futures):
        try:
            record=future.result();records[record['id']]=record;print(record['id'],record['license'],flush=True)
        except Exception as e:
            failed.append(futures[future][0]);print('FAILED',futures[future],e,flush=True)
credit_path.write_text(json.dumps(sorted(records.values(),key=lambda r:r['id']),ensure_ascii=False,indent=2))
if failed: raise SystemExit('Review failed photos: '+', '.join(failed))
