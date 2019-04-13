package industries.goodteam.gambit.effect

import industries.goodteam.gambit.StatType

// TODO: make this abstract for other effects
class Effect(
    val targetStat: StatType = StatType.STRENGTH,
    val value: Int = -1,
    val duration: Int = 1
) {

    fun apply(): AppliedEffect = AppliedEffect(targetStat, value, duration)

}