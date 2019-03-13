package industries.goodteam.gambit

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk27.coroutines.onClick

class MainActivity : AppCompatActivity() {

    lateinit var player: Character
    lateinit var enemy: Character

    lateinit var enemyAction: Action

    lateinit var enemyActionText: TextView

    lateinit var attackButton: Button
    lateinit var defendButton: Button
    lateinit var utilityButton: Button

    lateinit var playerHealthBar: ProgressBar
    lateinit var enemyHealthBar: ProgressBar

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set fullscreen options
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()

        setContentView(R.layout.combat)

        player = Character("player")
        enemy = Character("enemy")

        enemyActionText = find<TextView>(R.id.enemyActionText)

        attackButton = find<Button>(R.id.attackButton)
        defendButton = find<Button>(R.id.defendButton)
        utilityButton = find<Button>(R.id.utilityButton)

        playerHealthBar = find<ProgressBar>(R.id.healthBar).apply {
            max = player.health
            progress = player.health
        }
        enemyHealthBar = find<ProgressBar>(R.id.enemyHealthBar).apply {
            max = enemy.health
            progress = enemy.health
        }

        defendButton.onClick {
            act(Action.DEFEND)
        }
        attackButton.onClick {
            act(Action.ATTACK)
        }
        utilityButton.onClick {
            act(Action.UTILITY)
        }

        update(Action.random())
    }

    private fun act(action: Action) {
        if (action == Action.DEFEND) {
            player.resilience += 1
            defendButton.visibility = View.GONE
        }
        if (enemyAction == Action.DEFEND) enemy.resilience += 1

        if (action == Action.ATTACK) {
            enemy.health -= player.strength - enemy.resilience
            attackButton.visibility = View.GONE
        }
        if (enemyAction == Action.ATTACK) player.health -= enemy.strength - player.resilience

        if (action == Action.UTILITY) {
            player.utility()
            utilityButton.visibility = View.GONE
        }
        if (enemyAction == Action.UTILITY) enemy.utility()

        update(action)
    }

    private fun update(action: Action) {
        playerHealthBar.apply {
            max = player.vitality
            progress = player.health
        }
        enemyHealthBar.apply {
            max = enemy.vitality
            progress = enemy.health
        }
        player.resilience = 0
        enemy.resilience = 0
        enemyAction = Action.not(action)
        enemyActionText.text = enemyAction.name

        if (action != Action.DEFEND) defendButton.visibility = View.VISIBLE
        if (action != Action.ATTACK) attackButton.visibility = View.VISIBLE
        if (action != Action.UTILITY) utilityButton.visibility = View.VISIBLE
    }
}
