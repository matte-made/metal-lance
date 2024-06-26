package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import io.itch.mattekudasai.metallance.GlobalState.isPaused
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.withTransparency
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.graphics.use

class AutoPausingScreen<T>(private val screen: T) : KtxScreen, Disposing by Self(),
    KtxInputAdapter where T : KtxScreen, T : InputProcessor {


    private val spriteBatch: SpriteBatch by remember { SpriteBatch() }
    private val shapeRenderer: ShapeRenderer by remember { ShapeRenderer() }
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

    private val pauseMessage = listOf("CLICK HERE OR PRESS P TO PLAY")
    private val camera: Camera = OrthographicCamera()
    private val viewport: Viewport = FitViewport(0f, 0f, camera)
    private val pauseColor: Color = Color(0.0316f, 0.0316f, 0.0316f, 0.5f)
    private var ignoreNextResize = true

    init {
        screen.autoDisposing()
        Gdx.input.inputProcessor = this
    }

    private var deltaMultiplier: Float = 1f

    override fun render(delta: Float) {
        if (isPaused) {
            screen.render(0f)
            viewport.apply(true)
            withTransparency {
                shapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) {
                    it.color = pauseColor
                    it.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
                }
            }
            spriteBatch.use(camera) {
                textDrawer.drawText(it, pauseMessage, viewport.worldWidth / 2f, viewport.worldWidth / 2f, Align.top)
            }
        } else {
            if (Gdx.app.logLevel == Application.LOG_DEBUG) {
                screen.render(delta * deltaMultiplier)
            } else {
                screen.render(delta)
            }
        }
    }

    override fun pause() {
        isPaused = true
        screen.pause()
    }

    override fun resume() {
        screen.resume()
    }

    override fun show() {
        screen.show()
    }

    override fun resize(width: Int, height: Int) {
        if (!ignoreNextResize) {
            isPaused = true
        }
        ignoreNextResize = false
        viewport.setWorldSize(width.toFloat(), height.toFloat())
        viewport.setScreenSize(width, height)
        screen.resize(width, height)
    }

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Keys.P) {
            isPaused = !isPaused
            return true
        }
        if (Gdx.app.logLevel == Application.LOG_DEBUG) {
            if (keycode == Keys.LEFT_BRACKET) {
                deltaMultiplier /= 2f
            } else if (keycode == Keys.RIGHT_BRACKET) {
                deltaMultiplier *= 2f
            }
        }
        return screen.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return screen.keyUp(keycode)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (isPaused) {
            isPaused = false
        }
        return screen.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return screen.touchUp(screenX, screenY, pointer, button)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return screen.touchDragged(screenX, screenY, pointer)
    }
}
