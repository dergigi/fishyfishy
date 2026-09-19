# Madeira guide sources

Checked 2026-09-19. This is a small curated educational guide, not an exhaustive species list or an automated identification system. Short lessons are original summaries. Common names vary by region. The app displays all four names on each detail page and links to its species reference.

- Madeira occurrence and local names: [IFCN, Rocha do Navio natural values](https://ifcn.madeira.gov.pt/en/areas-protegidas/rocha-do-navio/valores-naturais.html), [Garajau fauna](https://ifcn.madeira.gov.pt/pt/?catid=68%3Afauna&id=181%3Arnp-garajau-funchal&view=article), [Madeira marine reserves](https://ifcn.madeira.gov.pt/images/Doc_Artigos/Divulgacao/publicacoes/livros/RESERVAS%20MARINHAS.pdf).
- Fish descriptions and feeding ecology: individual [FishBase](https://www.fishbase.se/) species references in `Species.kt`.
- Ornate wrasse: [DORIS](https://doris.ffessm.fr/Especes/Thalassoma-pavo-Girelle-paon-744).
- Canary damselfish: [Naturalis / Fishes of the Northeastern Atlantic and Mediterranean](https://fishes-fnam.linnaeus.naturalis.nl/linnaeus_ng/app/views/species/nsr_taxon.php?epi=141&id=109624). Older references call this species *Abudefduf luridus*; the guide uses *Similiparma lurida*.
- Common octopus: [FAO Cephalopods of the World, volume 3](https://www.fao.org/4/i3489e/i3489e.pdf).
- Common cuttlefish: [Marine Biological Association / MarLIN](https://www.marlin.ac.uk/species/detail/1098).
- Long-spined sea urchin: [Rodríguez et al., 2013, Zootaxa](https://www.mapress.com/zootaxa/2013/f/z03636p170f.pdf). This eastern Atlantic species is *Diadema africanum*, not the Caribbean *D. antillarum*.
- Purple sea star: [DORIS](https://doris.ffessm.fr/Especes/Ophidiaster-ophidianus-Etoile-de-mer-violette-670).

- Young Mediterranean damselfish and adult comparison: [DORIS](https://doris.ffessm.fr/Especes/Chromis-chromis-Castagnole-739), [MadeiraFish catalog](https://oomdata.arditi.pt/products/MadeiraFish/index.php?id=129&page=viewfish). Included as a lookalike, not a confirmed identification of the reported blue-striped fish. Compare with Canary damselfish and Azores chromis; a description alone is insufficient.
- Azores chromis: [University of Évora biodiversity museum](https://museubiodiversidade.uevora.pt/elenco-de-especies/biodiversidade-actual/animais/cordados/peixes/chromis-limbata/).
- Bogue: [DORIS](https://doris.ffessm.fr/Especes/Boops-boops-Bogue-2701).
- Sand smelt: [DORIS](https://doris.ffessm.fr/Especes/Atherina-presbyter-Pretre-423).
- European sardine: [DORIS](https://doris.ffessm.fr/Especes/Sardina-pilchardus-Sardine-d-Europe-3095), [MadeiraFish catalog](https://oomdata.arditi.pt/products/MadeiraFish/index.php?id=56&page=viewfish).

## Photos

All reference photos are bundled for offline use. These are real species photographs, not necessarily photographed in Madeira. Different sexes, ages, lighting and habitats can change appearance. Photos are not a complete identification key.

The exact file, author, original source URL, licence and changes for each photo are recorded in [`photo-credits.json`](../app/src/main/assets/photo-credits.json) and exposed from the creature detail screen. Photographs retain their individual CC BY / CC BY-SA licences; they are not covered by the app's MIT licence. Display crops and resized versions remain under the original photo's licence. Source files are available through the attributed Commons pages.

`scripts/fetch-guide-images.py` documents retrieval. Review image selection and attribution before replacing assets; Commons search results may change.
