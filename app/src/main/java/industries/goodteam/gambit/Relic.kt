package industries.goodteam.gambit

import industries.goodteam.gambit.property.Property

data class Relic(val name: String, val description: String = "", val properties: List<Property> = emptyList())