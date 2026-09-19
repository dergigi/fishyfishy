#!/usr/bin/env python3
"""Fetch redistributable reference photos with their Commons attribution records."""
import concurrent.futures, html, json, pathlib, re, urllib.parse, urllib.request
ROOT = pathlib.Path(__file__).resolve().parents[1]
SPECIES = {'wrasse':'Ornate wrasse','salema':'Salema porgy','parrotfish':'Sparisoma cretense','bream':'Diplodus sargus','zebra':'Diplodus cervinus','damselfish':'Similiparma lurida','comber':'Serranus atricauda','grouper':'Dusky grouper','octopus':'Octopus vulgaris','cuttlefish':'Common cuttlefish','urchin':'Diadema africanum','starfish':'Ophidiaster ophidianus'}
def get(url):
    req=urllib.request.Request(url,headers={'User-Agent':'FishyFishy/0.1 (Madeira educational field guide; https://github.com/dergigi)'})
    return urllib.request.urlopen(req,timeout=60).read()
def fetch(pair):
    key,title=pair
    if key in ('comber','urchin','starfish'):
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
    (ROOT/'app/src/main/res/drawable-nodpi'/f'{key}.jpg').write_bytes(get(image_url))
    return {'id':key,'file':filename,'author':clean(meta.get('Artist',{}).get('value','')),'license':license,'licenseUrl':meta.get('LicenseUrl',{}).get('value',''),'source':info['descriptionurl'],'imageUrl':image_url,'changes':'Resized by Wikimedia; displayed cropped in the app.'}
records=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    futures={pool.submit(fetch,p):p for p in SPECIES.items()}
    for future in concurrent.futures.as_completed(futures):
        try:
            record=future.result();records.append(record);print(record['id'],record['license'],flush=True)
        except Exception as e: print('FAILED',futures[future],e,flush=True)
records.sort(key=lambda r:r['id'])
(ROOT/'app/src/main/assets/photo-credits.json').write_text(json.dumps(records,ensure_ascii=False,indent=2))
print(f'{len(records)}/{len(SPECIES)} photos downloaded')
