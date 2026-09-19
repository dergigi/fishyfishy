package org.dergigi.fishyfishy

// Language and publisher checks documented in docs/DOCUMENTARIES.md.
private val germanCoast = GuideFilm(
    "Odeulw4kQS0", "Das älteste Meeresschutzgebiet Europas", "ARTE Fernweh",
    "Lebensraum Mittelmeerküste",
    "Entdecke das Meeresschutzgebiet Port-Cros im Mittelmeer. Vergleiche die Lebensräume mit Madeiras Küste. Eine längere Doku über den Lebensraum, nicht über jede einzelne Art.", "de",
)
val germanFilms = guide.associate { it.id to germanCoast }.toMutableMap().apply {
    put("octopus", GuideFilm("1PKNtvm4qTg", "Unterwasserkino für den Oktopus", "Wilde Tierwelt · BR",
        "Nina und die wilden Tiere", "Nina erforscht in Kroatien, wie geschickt und neugierig Oktopusse sind. Beobachte ihre Arme und ihre Bewegungen.", "de"))
    val aquarium = GuideFilm("sTWZA3ogLr8", "Meeresfische", "Wilde Tierwelt · BR",
        "Anna und die Haustiere", "Anna besucht ein Meerwasseraquarium mit Fischen, Garnelen und Anemonen. Es zeigt verwandte Tiere aus anderen Meeren, nicht die Arten unseres Madeira-Führers.", "de")
    listOf("cleaner_shrimp", "club_anemone", "snakelocks").forEach { put(it, aquarium) }
    val meadow = GuideFilm("QJDBrXvpQPo", "Abtauchen in die Seegraswiese", "Wilde Tierwelt · BR",
        "Pia und die wilde Natur", "Pia erkundet eine Seegraswiese im Mittelmeer. Vergleiche diesen Lebensraum mit den Felsen und Algen bei unseren Schnorcheltouren.", "de")
    listOf("salema", "sand_bream", "cuttlefish", "sand_smelt", "bogue").forEach { put(it, meadow) }
}.toMap()

private fun portugueseFilm(id: String, title: String, topic: String, note: String) =
    GuideFilm(id, title, "CEBIMar · USP", topic, note, "pt")
private val portugueseCoast = portugueseFilm("9xDGw0Oyge4", "Brasil debaixo d’água · Banco de rodolitos",
    "Habitats marinhos do Brasil", "Um pequeno filme educativo em português do Brasil sobre fundos com algas calcárias. Compara este habitat com os fundos da Madeira; não é um filme sobre cada espécie do nosso guia.")
val portugueseFilms = guide.associate { it.id to portugueseCoast }.toMutableMap().apply {
    fun link(ids: List<String>, video: String, title: String, topic: String, note: String) {
        val film = portugueseFilm(video, "Brasil debaixo d’água · $title", topic, note)
        ids.forEach { put(it, film) }
    }
    link(listOf("octopus", "cuttlefish"), "k0ncnGX8MgQ", "Polvo-comum", "Polvos do Brasil",
        "Conhece um polvo da costa brasileira neste pequeno filme educativo. Compara os seus braços e a camuflagem com os polvos e chocos da Madeira.")
    link(listOf("sponge"), "tH6aN2oTJag", "Esponja-tubular-amarela", "Esponjas aparentadas",
        "Uma esponja tubular do Brasil, aparentada com as nossas esponjas. Compara os tubos e as aberturas; a espécie pode ser diferente.")
    link(listOf("sardine", "bogue", "sand_smelt"), "G62e-HbdCQE", "Sardinha", "Peixes de cardume",
        "Observa sardinhas do Brasil e compara o cardume com os que vês na Madeira. O mesmo nome comum pode designar espécies diferentes.")
    link(listOf("cleaner_shrimp"), "O8bLjd0CtkE", "Camarão-limpador-branco", "Camarões aparentados",
        "Conhece um camarão-limpador do Brasil. Compara-o com o nosso camarão-limpador; não é a mesma espécie.")
    link(listOf("parrotfish"), "Hu8dCM62Ekk", "Peixe-papagaio-vermelho", "Parentes do bodião",
        "Conhece um peixe-papagaio do Brasil, aparentado com o bodião da Madeira. Compara a forma do corpo e o bico.")
    link(listOf("black_moray", "moray"), "xdJ26AeOOKs", "Moreia-pintada", "Moreias aparentadas",
        "Uma moreia da costa brasileira. Compara a forma e as manchas com as nossas moreias; os nomes comuns podem referir-se a espécies diferentes.")
    link(listOf("club_anemone", "snakelocks"), "piBLLU2cODo", "Anémona-de-tubo", "Parentes das anémonas",
        "Observa os tentáculos de uma anémona-de-tubo do Brasil. É um animal aparentado, diferente das anémonas do nosso guia.")
    link(listOf("triggerfish"), "2O8paieEZKI", "Peixe-porco-pintado", "Parentes do peixe-porco",
        "Conhece um peixe-porco do Brasil, de uma espécie diferente da nossa. Compara o corpo e as barbatanas.")
    link(listOf("grouper", "comber"), "B9_fCZyC49Y", "Badejo-ferro", "Peixes aparentados",
        "Conhece um badejo da costa brasileira, aparentado com meros e garoupas. É uma espécie diferente das do nosso guia.")
    link(listOf("fireworm"), "WpDPK4ZsPZk", "Poliqueta-árvore-de-natal", "Vermes marinhos aparentados",
        "Este pequeno verme marinho vive num tubo e é parente do verme-de-fogo. Compara formas de viver muito diferentes.")
    val algae = portugueseFilm("f5YJpfw6mgU", "Brasil debaixo d’água · Peixes nas algas", "Habitats marinhos do Brasil",
        "Observa peixes entre as algas na costa brasileira. Compara o habitat com o da Madeira; este filme mostra animais de outro lugar.")
    listOf("wrasse", "salema", "bream", "zebra", "damselfish", "chromis", "azores_chromis", "saddled", "two_banded", "mullet", "goatfish", "lizardfish", "triplefin", "redlip", "toby", "puffer", "scorpionfish", "trumpetfish", "barracuda", "sand_bream").forEach { put(it, algae) }
}.toMap()
