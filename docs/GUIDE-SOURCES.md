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

## Coastal coverage review for 0.3.0

The guide now has 47 entries, including the explicitly labelled *Chromis chromis* lookalike. It is a practical shallow-water guide, not a claim to cover every common animal in the archipelago. Abundance changes with shore, habitat, depth and season; a coastal record does not guarantee a snorkeling encounter. Deeper-water fish, offshore wildlife and many difficult-to-separate gobies, blennies, molluscs and small invertebrates remain outside this edition.

Selection was checked against these local sources rather than inferring Madeira abundance from Mediterranean or Azorean surveys:

- [Ribeiro: Madeira rocky inshore reef surveys](https://repositorio.ulisboa.pt/entities/publication/1fcc6098-7276-4f6f-81f9-d5bfb1f4bff1), including the [full thesis](https://repositorio.ulisboa.pt/bitstreams/37aec0b3-f53b-4806-9567-9f90515e95fc/download): identifies frequent reef residents, including the two puffers, Madeira rockfish, triplefin, lizardfish and two-banded and saddled seabreams.
- [IFCN Garajau natural values](https://ifcn.madeira.gov.pt/en/areas-protegidas/garajau/valores-naturais.html) and [Garajau fauna](https://ifcn.madeira.gov.pt/pt/?catid=68%3Afauna&id=181%3Arnp-garajau-funchal&view=article): local morays, crustaceans, urchins, anemones, sponges and fireworms; the page also distinguishes seasonal visiting fish.
- [Garajau environmental report](https://ifcn.madeira.gov.pt/images/Doc_Artigos/Garajau/PEOGRNPG.pdf): golden grey mullet, saddled seabream, red rock crab and rocky-shore communities.
- [AMACO/IFCN 2023 invertebrate survey](https://amaco.pt/wp-content/uploads/2024/10/05_Relatorio_IFCN_2023_final-optimized.pdf): Madeira and Porto Santo sampling, including *Percnon gibbesi*, *Calcinus tubularis*, cleaner shrimp and sea cucumbers. Occurrence and local abundance are not interchangeable with ease of seeing them from the surface.
- [Ichthyofauna of Portugal](https://www.vliz.be/imisdocs/publications/341518.pdf): regional occurrence and Portuguese fish names, including *Canthigaster capistrata*, which should not be replaced by the Caribbean *C. rostrata*.
- [MadeiraFish rough limpet](https://oomdata.arditi.pt/products/MadeiraFish/index.php?id=241&page=viewfish) and [Madeira fisheries: limpets](https://marmadeira.madeira.gov.pt/lapas-e-caramujos/).

Species-specific identification and biology links are in `CoastalGuide.kt`, using FishBase, DORIS, MarLIN and local sources. Useful comparisons include [black-faced triplefin](https://doris.ffessm.fr/ref/specie/823), [Guinean puffer](https://ciesm.org/atlas/fishes_2nd_edition/Sphoeroides_marmoratus.pdf), [fireworm](https://doris.ffessm.fr/ref/specie/882), [cleaner shrimp](https://doris.ffessm.fr/ref/specie/3926), [sea cucumber](https://doris.ffessm.fr/ref/specie/1744), [club-tipped anemone](https://doris.ffessm.fr/ref/specie/1267), [snakelocks anemone](https://doris.ffessm.fr/ref/specie/111), [tube-dwelling hermit crab](https://doris.ffessm.fr/ref/specie/685), [purple urchin](https://www.marlin.ac.uk/species/detail/1499) and [spiny sea star](https://www.marlin.ac.uk/species/detail/1688).

Older local documents use *Liza aurata* for *Chelon auratus*, *Anemonia sulcata* for *A. viridis*, and *Grapsus grapsus* for the eastern Atlantic crab now treated as *G. adscensionis*. Their outdated *Diadema antillarum* name is not carried into the app. English, Portuguese and German names are learning labels, not unique taxonomic identifiers. Some less familiar invertebrates use descriptive translated labels; the scientific name is the precise reference. Common labels such as “bernardo-eremita” and “caranguejo-aranha” cover more than one species. The guide asks users to keep difficult comparisons uncertain.

Thirty new photos were selected individually from Commons and visually reviewed. Living animals in habitat are preferred. Reference photos can be from elsewhere in the species' range; they do not constitute Madeira occurrence evidence. Full-screen viewing preserves the whole image and its author/licence attribution.

## Identification photo comparisons

The detail gallery keeps each photo's own credit and source link. All images are bundled for offline use. The additional comparison images and caption evidence are:

- **Mediterranean parrotfish:** existing grey male plus a red female. [DORIS species description](https://doris.ffessm.fr/Especes/Sparisoma-cretense-Poisson-perroquet-mediterraneen-4462) describes the colour differences; [female photo metadata](https://commons.wikimedia.org/wiki/File:Sparisoma_cretense_2_(cropped).jpg) explicitly identifies a female.
- **Ornate wrasse:** existing female-pattern photograph, [male photograph](https://commons.wikimedia.org/wiki/File:Thalassoma_pavo_male.jpg), and [juvenile photograph](https://commons.wikimedia.org/wiki/File:Juv%C3%A9nile_de_Girelle_paon.jpg). [DORIS](https://doris.ffessm.fr/Especes/Thalassoma-pavo-Girelle-paon-744) distinguishes initial and terminal colour phases: initial males can resemble females. Captions therefore describe colour patterns rather than claiming that every striped fish is female. The small juvenile is shown among seaweed; zoom helps locate it.
- **Salema:** an additional school photographed in Madeira; location and species are documented in the linked Commons attribution record.
- Existing Canary damselfish alternate view and Mediterranean damselfish juvenile/adult comparison use the same swipe gallery. The latter retains its lookalike caveat.

Photos are examples, not an exhaustive catalogue of every age, sex or seasonal colour. Sex is not inferred for other reference photographs. Commons filenames, authors, licences and source URLs are retained in `app/src/main/assets/photo-credits.json`; curated downloads are reproducible through `scripts/fetch-guide-images.py`.
