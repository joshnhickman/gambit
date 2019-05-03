package industries.goodteam.gambit

enum class Stat {

    LUCK,
    VITALITY,
    HEALTH,
    STRENGTH,
    ACCURACY,
    ARMOR,
    REFLEXES,
    CONCENTRATION;

    override fun toString(): String = name.toLowerCase()

}