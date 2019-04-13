package industries.goodteam.gambit.effect

import industries.goodteam.gambit.StatType

class AppliedEffect(
    val targetStat: StatType,
    val value: Int,
    var left: Int
) {

    fun update() {
        left--
    }

    fun done(): Boolean = left < 0

}