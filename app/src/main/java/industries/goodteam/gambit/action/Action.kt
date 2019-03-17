package industries.goodteam.gambit.action

abstract class Action(
    val name: String,
    var cooldown: Int
) {

    private var left = -1

    fun update() {
        if (!ready()) left--
    }

    fun perform() {
        if (!ready()) throw IllegalStateException("cooldown is not finished")
        left = cooldown
    }

    fun ready(): Boolean = left < 0

}