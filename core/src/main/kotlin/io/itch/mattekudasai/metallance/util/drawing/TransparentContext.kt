package io.itch.mattekudasai.metallance.util.drawing

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

fun withTransparency(block: () -> Unit) {
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    block()
    Gdx.gl.glDisable(GL20.GL_BLEND);
}
