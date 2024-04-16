package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.GlobalState.isPaused
import io.itch.mattekudasai.metallance.player.Controls.isAnyKey
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.DelayedTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.withTransparency
import io.itch.mattekudasai.metallance.util.files.overridable
import io.itch.mattekudasai.metallance.util.pixel.intFloat
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use
import kotlin.math.max
import kotlin.math.min

class IntroScreen(val finish: () -> Unit) : KtxScreen, KtxInputAdapter, Disposing by Self() {

    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)

    private val textDrawer: MonoSpaceTextDrawer by remember {
        MonoSpaceTextDrawer(
            fontFileName = "texture/font_white.png",
            alphabet = ('A'..'Z').joinToString(separator = "") + ".,'0123456789:",
            fontLetterWidth = 5,
            fontLetterHeight = 9,
            fontHorizontalSpacing = 1,
            fontVerticalSpacing = 0,
            fontHorizontalPadding = 1,
        )
    }
    private val delayedTextDrawer = DelayedTextDrawer(textDrawer, characterTime)
    private val textures = images.map { Texture(it.overridable).autoDisposing() }
    private val music: Music by remember { Gdx.audio.newMusic("music/intro.ogg".overridable) }
    private var fadeInFor: Float = 0f
    private var fadeOutFor: Float = 0f

    private val sequence = listOf(
        0.5f to {}, // wait
        0f to {
            if (textIndex < textArray.size) {
                fadeInFor = FADE_TIME
                val text = textArray[textIndex++]
                val textDelay = text.sumOf { it.length } * characterTime + 2f
                delayedTextDrawer.startDrawing(text, viewport.worldWidth / 2f, 0f)
                actionIndex--
                currentWaitTime += textDelay
            }
        },
        1f to {
            fadeOutFor = FADE_TIME
          }, // wait
        0f to {
            music.stop()
            finish()
        }
    )
    private var textIndex = 0
    private var actionIndex = 0
    private var currentWaitTime = 0f
    private val transparentColor = Color.WHITE.cpy()

    override fun render(delta: Float) {
        clearScreen(red = 0f, green = 0f, blue = 0f)
        if (actionIndex >= sequence.size) {
            return
        }

        if (delta == 0f) {
            music.pause()
        } else if (!music.isPlaying) {
            music.play()
            music.volume = musicVolume
            music.isLooping = true
        }

        if (fadeInFor > 0f) {
            fadeInFor = max(0f, fadeInFor - delta)
            transparentColor.a = min(1f, 1f - fadeInFor / FADE_TIME)
        }
        if (fadeOutFor > 0f) {
            fadeOutFor = max(0f, fadeOutFor - delta)
            transparentColor.r = 1f
            transparentColor.g = 1f
            transparentColor.b = 1f
            transparentColor.a = max(0f, fadeOutFor / FADE_TIME)
            music.volume = musicVolume * transparentColor.a
        }

        currentWaitTime -= delta
        while (currentWaitTime < 0f && actionIndex < sequence.size) {
            val action = sequence[actionIndex++]
            action.second.invoke()
            currentWaitTime += action.first
        }

        if (actionIndex == 0) {
            return
        }

        viewport.apply(true)
        withTransparency {
            batch.use(camera) {
                if (textIndex - 1 >= 0) {
                    if (actionIndex == 1) {
                        if (transparentColor.a < 1f && textIndex - 2 >= 0) {
                            it.color = Color.WHITE
                            it.draw(textures[textIndex - 2], 0f.intFloat, 32f.intFloat)
                            transparentColor.r = transparentColor.a
                            transparentColor.g = transparentColor.a
                            transparentColor.b = transparentColor.a
                        } else {
                            transparentColor.r = 1f
                            transparentColor.g = 1f
                            transparentColor.b = 1f
                        }
                    }
                    it.color = transparentColor
                    it.draw(textures[textIndex - 1], 0f.intFloat, 32f.intFloat)

                }
                it.color = if (actionIndex > 1) transparentColor else Color.WHITE
                delayedTextDrawer.updateAndDraw(delta, batch)
            }
        }
        super.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        viewport.setWorldSize(width.toFloat(), height.toFloat())
        viewport.setScreenSize(width, height)
    }

    override fun keyDown(keycode: Int): Boolean {
        if (isPaused) {
            return false
        }
        if (keycode.isAnyKey) {
            music.stop()
            finish()
        }
        return true
    }

    companion object {
        private const val musicVolume = 1f
        private const val characterTime = 0.04f
        private val lines = """
        20XX

        Earth is invaded by alien race
        That conquered the universe""".trimIndent().uppercase()

        private val textArray = lines.split("\n\n").map { it.split("\n") }
        private val images = textArray.indices.map { "texture/intro/intro$it.png" }
        private const val FADE_TIME = 0.5f
    }

}
