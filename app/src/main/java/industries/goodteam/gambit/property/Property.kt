package industries.goodteam.gambit.property

sealed class Property {
    abstract fun describe(): String
    abstract fun describeShort(): String
}

class Retaliate(val value: Int = 1) : Property() {
    override fun describe(): String = "retaliate for $value damage"
    override fun describeShort(): String = "retaliate $value"
}