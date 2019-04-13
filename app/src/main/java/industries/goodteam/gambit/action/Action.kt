package industries.goodteam.gambit.action

abstract class Action(
    val name: String,
    var cooldown: Int,
    var start: Int = -1
) {

    private var left = start

    open fun update() {
        if (!ready()) left--
    }

    fun refresh() {
        left = -1
    }

    open fun perform() {
        if (!ready()) throw IllegalStateException("cooldown is not finished")
        left = cooldown
    }

    fun ready(): Boolean = left < 0

}