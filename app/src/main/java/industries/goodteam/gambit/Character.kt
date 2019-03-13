package industries.goodteam.gambit

data class Character(
    val name: String,
    var vitality: Int = 10,
    var strength: Int = 1,
    var resilience: Int = 0
) {

    var health = vitality
    lateinit var nextAction: Action

    fun act() {

    }

    fun utility() {
        strengthen()
    }

    fun heal(amount: Int = 1) {
        health += amount
        if (health > vitality) health = vitality
    }

    fun strengthen(amount: Int = 1) {
        strength += 1
    }

}