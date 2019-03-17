package industries.goodteam.gambit.entity

import industries.goodteam.gambit.action.*


open class Entity(
    var name: String,
    var vitality: Int,
    var strength: Int,
    var armor: Int,
    var concentration: Int,
    vararg var actions: Action
) {

    var health = vitality
    var shield = 0

    var intent = actions[0]

    private var stunLeft = -1

    open fun update() {
        shield = 0
        for (action in actions) {
            action.update()
        }

        if (stunned()) stunLeft--
    }

    open fun intend(action: Action? = null) {
        intent = when {
            stunned() -> Wait()
            action != null -> action
            else -> actions.filter { it.ready() }.random()
        }
    }

    open fun act(action: Action = intent) {
        intent.perform()
    }

    open fun defend() {
        shield = armor
    }

    open fun hit(damage: Int): Int {
        var actualDamage = damage - shield
        if (actualDamage < 0) actualDamage = 0
        if (health - actualDamage >= 0) health -= actualDamage else health = 0
        return actualDamage
    }

    open fun stun(duration: Int): Int {
        stunLeft = duration
        intend(Wait())
        return duration
    }

    open fun alive(): Boolean = health > 0

    open fun stunned(): Boolean = stunLeft > -1

    fun actionValue(action: Action = intent): Int {
        when(action) {
            is Attack -> return strength
            is Defend -> return armor
            is Stun -> return concentration
            is Wait -> return stunLeft
            else -> return 0
        }
    }

}