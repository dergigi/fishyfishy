package org.dergigi.fishyfishy

data class GuideFilm(val videoId: String, val title: String, val publisher: String, val topic: String, val note: String) {
    val url: String get() = "https://www.youtube.com/watch?v=$videoId"
}

// Direct links checked against YouTube publisher metadata. See docs/DOCUMENTARIES.md.
val guideFilms: Map<String, GuideFilm> = buildMap {
    listOf("octopus").forEach { id ->
        put(id, GuideFilm("ebeNeQFUMa0", "Extraordinary Octopus Takes To Land | The Hunt | BBC Earth", "BBC Earth", "Octopus relatives", "Watch a shore-hunting octopus explore its coastal home. Compare its movements with the octopuses you spot."))
    }
    listOf("cuttlefish").forEach { id ->
        put(id, GuideFilm("pgDE2DOICuc", "Can Cuttlefish camouflage in a living room? | Richard Hammond's Miracles of Nature - BBC", "BBC", "Cuttlefish", "A short film exploring how cuttlefish match their surroundings. Look for the changing skin patterns."))
    }
    listOf("parrotfish").forEach { id ->
        put(id, GuideFilm("o-blz2ghKOU", "Feeding Humphead Parrotfish | Blue Planet | BBC Earth", "BBC Earth", "Parrotfish relatives", "Meet tropical humphead parrotfish. These are relatives of our Mediterranean parrotfish, with different feeding habits."))
    }
    listOf("toby", "puffer").forEach { id ->
        put(id, GuideFilm("1k0MMxhOVpA", "Pufferfish Builds Sand Sculpture for Mating", "Nature on PBS", "Pufferfish relatives", "Meet a Japanese pufferfish that builds beautiful sand nests: a different species from the puffers we see in Madeira."))
    }
    listOf("sardine", "bogue", "sand_smelt").forEach { id ->
        put(id, GuideFilm("0zkvqdw_r4A", "Epic Journey of Sardines | BBC Studios", "BBC Studios", "Schooling fish", "Follow South Africa’s sardine run and watch how a huge school moves together. Compare it with the silver shoals you see."))
    }
    listOf("starfish", "spiny_star").forEach { id ->
        put(id, GuideFilm("9rxf_2EgwfE", "Ever Seen a Starfish Gallop? | Deep Look", "Deep Look", "Sea star relatives", "Discover how sea stars move with their tube feet. The film features relatives of Madeira’s sea stars."))
    }
    listOf("urchin", "black_urchin", "purple_urchin").forEach { id ->
        put(id, GuideFilm("ak2xqH5h0YY", "Sea Urchins Pull Themselves Inside Out to be Reborn | Deep Look", "Deep Look", "Sea urchin relatives", "Follow the remarkable early life of sea urchins. The featured species are different from our local urchins."))
    }
    listOf("rock_crab", "nimble_crab", "arrow_crab").forEach { id ->
        put(id, GuideFilm("OwQcv7TyX04", "Decorator Crabs Make High Fashion at Low Tide | Deep Look", "Deep Look", "Crab relatives", "Meet decorator crabs, relatives of our local crabs, that dress in seaweed to blend into their surroundings."))
    }
    listOf("hermit_crab").forEach { id ->
        put(id, GuideFilm("f1dnocPQXDQ", "Crabs Trade Shells in the Strangest Way | BBC Earth", "BBC Earth", "Hermit crab relatives", "These land hermit crabs exchange shells. Our tiny underwater hermit crab is a different species with a different way of life."))
    }
    listOf("cleaner_shrimp").forEach { id ->
        put(id, GuideFilm("RQ3YFmdIB6M", "These Shrimp Are a Clean-Up Crew For Dirty Fish | Nat Geo Wild", "Nat Geo Animals", "Cleaner shrimp relatives", "See Pederson cleaner shrimp tending fish in the Caribbean. They are relatives of our local cleaner shrimp."))
    }
    listOf("club_anemone", "snakelocks").forEach { id ->
        put(id, GuideFilm("HZIl16ttenY", "Cnidarians: Squishy Stingy animals! | JONATHAN BIRD'S BLUE WORLD", "BlueWorldTV", "Anemones and relatives", "Explore the animal group that includes anemones, corals and jellyfish. This is a broader look at their relatives."))
    }
    listOf("sponge").forEach { id ->
        put(id, GuideFilm("m8a0oNsDEx8", "Sponges! | JONATHAN BIRD'S BLUE WORLD", "BlueWorldTV", "Sponge relatives", "Learn how sponges live and filter water. Discover relatives of our yellow tube sponge."))
    }
    listOf("sea_cucumber").forEach { id ->
        put(id, GuideFilm("-plc6r2KOD0", "A Living Tube With Little Tube Feet", "Bizarre Beasts", "Sea cucumber relatives", "Meet a sea pig, a deep-sea relative of our brown sea cucumber. Compare their shapes and tiny tube feet."))
    }
    listOf("wrasse", "salema", "bream", "zebra", "damselfish", "comber", "grouper", "chromis", "azores_chromis", "saddled", "two_banded", "mullet", "goatfish", "lizardfish", "triplefin", "redlip", "scorpionfish", "black_moray", "moray", "trumpetfish", "triggerfish", "barracuda", "sand_bream", "fireworm", "limpet").forEach { id ->
        put(id, GuideFilm("jVn8ZXaditg", "The Gorgeous Wildlife of the Mediterranean | Free Documentary Nature", "Free Documentary - Nature", "Coastal habitat", "Explore Mediterranean coastal habitats and compare them with Madeira’s rocky shores. This longer documentary is filmed outside Madeira."))
    }
}
