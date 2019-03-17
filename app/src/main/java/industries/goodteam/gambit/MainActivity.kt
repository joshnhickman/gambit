package industries.goodteam.gambit

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import industries.goodteam.gambit.action.*
import industries.goodteam.gambit.entity.Entity
import org.jetbrains.anko.find
import org.jetbrains.anko.longToast
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    val attack = Attack(0)
    val defend = Defend(1)
    val stun = Stun(2)

    lateinit var player: Entity
    lateinit var enemy: Entity

    lateinit var enemyNameText: TextView
    lateinit var enemyActionText: TextView

    lateinit var defendButton: Button
    lateinit var playerArmor: EditText

    lateinit var attackButton: Button
    lateinit var stunButton: Button

    lateinit var attackText: TextView
    lateinit var defendText: TextView
    lateinit var stunText: TextView

    lateinit var playerHealthBar: ProgressBar
    lateinit var enemyHealthBar: ProgressBar

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set fullscreen options
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()

        setContentView(R.layout.combat)

        init()

        defendButton = find<Button>(R.id.defendButton).apply { onClick { act(defend) } }
        find<EditText>(R.id.playerArmor).apply {
            setText("${player.armor}")
            afterTextChanged { player.armor = it.getIntWithBlank() }
        }
        find<EditText>(R.id.defendCooldown).apply {
            setText("${defend.cooldown}")
            afterTextChanged { defend.cooldown = it.getIntWithBlank(); draw() }
        }

        attackButton = find<Button>(R.id.attackButton).apply { onClick { act(attack) } }
        find<EditText>(R.id.playerStrength).apply {
            setText("${player.strength}")
            afterTextChanged { player.strength = it.getIntWithBlank(); draw() }
        }
        find<EditText>(R.id.attackCooldown).apply {
            setText("${attack.cooldown}")
            afterTextChanged { attack.cooldown = it.getIntWithBlank(); draw() }
        }

        stunButton = find<Button>(R.id.utilityButton).apply { onClick { act(stun) } }
        find<EditText>(R.id.playerConcentration).apply {
            setText("${player.concentration}")
            afterTextChanged { player.concentration = it.getIntWithBlank(); draw() }
        }
        find<EditText>(R.id.stunCooldown).apply {
            setText("${stun.cooldown}")
            afterTextChanged { stun.cooldown = it.getIntWithBlank(); draw() }
        }

        playerHealthBar = find<ProgressBar>(R.id.healthBar)
        enemyHealthBar = find<ProgressBar>(R.id.enemyHealthBar)

        enemyNameText = find<TextView>(R.id.enemyNameText)
        enemyActionText = find<TextView>(R.id.enemyActionText)

        find<EditText>(R.id.enemyArmor).apply {
            setText("${enemy.armor}")
            afterTextChanged { enemy.armor = it.getIntWithBlank(); draw() }
        }
        find<EditText>(R.id.enemyStrength).apply {
            setText("${enemy.strength}")
            afterTextChanged { enemy.strength = it.getIntWithBlank(); draw() }
        }

        update()
    }

    private fun init() {
        player = Entity("player", 40, 4, 4, 2, attack, defend, stun)
        enemy = Entity("enemy", 30, 5, 5, 0, Attack(0), Defend(1))
    }

    private fun act(action: Action) {
        player.intend(action)

        if (action is Stun) {
            var duration = enemy.stun(player.concentration)
            toast("you stunned ${enemy.name} for ${duration} turns")
        }
        if (enemy.intent is Stun) {
            var duration = player.intend(Wait())
            toast("${enemy.name} stunned you for ${duration} turns")
        }

        if (action is Defend) player.defend()
        if (enemy.intent is Defend) enemy.defend()

        if (action is Attack) {
            var damage = enemy.hit(player.actionValue())
            toast("you dealt ${damage} damage to ${enemy.name}")
        }
        if (enemy.intent is Attack) {
            var damage = player.hit(enemy.actionValue())
            toast("${enemy.name} dealt ${damage} damage to you")
        }

        player.act()
        enemy.act()

        if (!enemy.alive()) {
            longToast("WINNER")
            init()
        } else if (!player.alive()) {
            longToast("LOSER")
            init()
        }

        update()
    }

    private fun update() {
        player.update()
        enemy.update()

        enemy.intend()

        draw()
    }

    private fun draw() {
        playerHealthBar.apply {
            max = player.vitality
            progress = player.health
        }
        enemyHealthBar.apply {
            max = enemy.vitality
            progress = enemy.health
        }

        enemyNameText.text = enemy.name
        enemyActionText.text = "${enemy.intent.name} ${enemy.actionValue()}"

        if (player.stunned()) {
            defendButton.visibility = View.GONE
            attackButton.visibility = View.GONE
            stunButton.visibility = View.GONE
        } else {
            defendButton.visibility = if (defend.ready()) View.VISIBLE else View.GONE
            attackButton.visibility = if (attack.ready()) View.VISIBLE else View.GONE
            stunButton.visibility = if (stun.ready()) View.VISIBLE else View.GONE
        }
    }

    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                afterTextChanged.invoke(editable.toString())
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    fun String.getIntWithBlank(): Int {
        try {
            return this.toInt()
        } catch (e: NumberFormatException) {
            return 0
        }
    }
}
