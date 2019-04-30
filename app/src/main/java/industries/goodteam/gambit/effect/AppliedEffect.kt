package industries.goodteam.gambit.effect

import industries.goodteam.gambit.StatType

class AppliedEffect(
    val effect: Effect,
    val targetStat: StatType = effect.targetStat,
    val value: Int = effect.value,
    val duration: Int = effect.duration,
    var left: Int = duration
) {

    fun update() {
        left--
    }

    fun done(): Boolean = left < 0

}