import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.KeyboardEvent

enum class Status {
    STOPPED, RUNNING, GAMEOVER
}

enum class Keys(val value: Int) {
    LEFT(37), RIGHT(39)
}

data class Ball(
    var speed: Int = 5,
    var x: Int = 135,
    var y: Int = 100,
    var directionX: Int = -1,
    var directionY: Int = -1
)

data class Pong(
    var status: Status = Status.STOPPED,
    var ball: Ball = Ball(),
    var score: Int = 0,
    var loopInterval: Int = 16,
    var pressedKeys: MutableSet<Int> = mutableSetOf()
) {
    fun isRunning() = status == Status.RUNNING
    fun isStopped() = status == Status.STOPPED
    fun isGameOver() = status == Status.GAMEOVER
}

fun loop(
    pong: Pong,
    playgroundHtml: HTMLElement,
    racketHtml: HTMLElement,
    ballHtml: HTMLElement,
    scoreHtml: HTMLElement,
    gameOverHtml: HTMLElement
) {
    if (pong.isRunning()) {
        val newDirectionX = moveBallDirectionX(playgroundHtml, pong.ball)
        val newDirectionY = moveBallDirectionY(playgroundHtml, pong.ball)
        val newPosX = moveBallPosition(pong.ball, newDirectionX)
        val newPosY = moveBallPosition(pong.ball, newDirectionY)
        changeBallPosition(pong.ball, newDirectionX, newPosX, newDirectionY, newPosY)
        drawBall(pong.ball, ballHtml)
        val pixelPos = moveRacket(racketHtml, pong)
        drawRacket(racketHtml, pixelPos)
        val hit = isRacketHit(racketHtml, ballHtml, pong.ball)
        val newScore = computeScore(pong, hit)
        changeDirectionY(pong, hit)
        pong.score = newScore
        drawScore(scoreHtml, newScore)
        endGame(pong, racketHtml, ballHtml)
        drawEndGame(pong, gameOverHtml)
    }
}

fun drawEndGame(pong: Pong, gameOverHtml: HTMLElement) {
    if (pong.isGameOver()) {
        gameOverHtml.style.display = "block"
    }
}

fun endGame(pong: Pong, racketHtml: HTMLElement, ballHtml: HTMLElement) {
    val isGameOver: () -> Boolean = {
        val bottomPos = racketHtml.offsetHeight
        val posY = nextPosition(pong.ball.y, pong.ball.speed, pong.ball.directionY) - bottomPos
        val racketPosY = racketPositionY(racketHtml, ballHtml)
        posY > racketPosY
    }
    if (isGameOver()) {
        pong.status = Status.GAMEOVER
    }
}

fun drawScore(scoreHtml: HTMLElement, score: Int) {
    scoreHtml.innerHTML = score.toString()
}

fun changeDirectionY(pong: Pong, hit: Boolean) {
    if (hit) {
        pong.ball.directionY = -1
    }
}

fun computeScore(pong: Pong, hit: Boolean): Int {
    if (hit) {
        return pong.score + 1
    }
    return pong.score
}

fun isRacketHit(racketHtml: HTMLElement, ballHtml: HTMLElement, ball: Ball): Boolean {
    val posX = nextPosition(ball.x, ball.speed, ball.directionX)
    val posY = nextPosition(ball.y, ball.speed, ball.directionY)
    val racketPosY = racketPositionY(racketHtml, ballHtml)
    val racketBorderLeft = racketHtml.offsetLeft
    val racketBorderRight = racketBorderLeft + racketHtml.offsetWidth
    return posX >= racketBorderLeft && posX <= racketBorderRight && posY >= racketPosY
}

fun racketPositionY(racketHtml: HTMLElement, ballHtml: HTMLElement): Int {
    return racketHtml.offsetTop - ballHtml.offsetHeight // subtracting ball's size to avoid passing through the racket
}

fun drawRacket(racketHtml: HTMLElement, pixelPos: Int) {
    racketHtml.style.left = "${pixelPos}px"
}

fun moveRacket(racketHtml: HTMLElement, pong: Pong) =
    if (pong.pressedKeys.contains(Keys.LEFT.value)) {
        racketHtml.offsetLeft - 5
    } else if (pong.pressedKeys.contains(Keys.RIGHT.value)) {
        racketHtml.offsetLeft + 5
    } else {
        racketHtml.offsetLeft
    }

fun drawBall(ball: Ball, ballHtml: HTMLElement) {
    ballHtml.style.left = "${ball.x}px"
    ballHtml.style.top = "${ball.y}px"
}

fun changeBallPosition(ball: Ball, directionX: Int, positionX: Int, directionY: Int, positionY: Int) {
    ball.directionX = directionX
    ball.directionY = directionY
    ball.x += positionX
    ball.y += positionY
}

fun moveBallPosition(ball: Ball, direction: Int): Int = ball.speed * direction

fun moveBallDirectionX(playgroundHtml: HTMLElement, ball: Ball): Int {
    val width = playgroundHtml.offsetWidth
    var directionX = ball.directionX
    val posX = nextPosition(ball.x, ball.speed, ball.directionX)
    if (posX > width) directionX = -1
    if (posX < 0) directionX = 1
    return directionX
}

fun moveBallDirectionY(playgroundHtml: HTMLElement, ball: Ball): Int {
    val height = playgroundHtml.offsetHeight
    var directionY = ball.directionY
    val posY = nextPosition(ball.y, ball.speed, ball.directionY)
    if (posY > height) directionY = -1
    if (posY < 0) directionY = 1
    return directionY
}

fun nextPosition(currentPosition: Int, speed: Int, direction: Int): Int = currentPosition + speed * direction

fun load(pong: Pong) {
    val playgroundHtml = document.getElementById("playground") as HTMLElement
    val racketHtml = document.getElementById("racket") as HTMLElement
    val ballHtml = document.getElementById("ball") as HTMLElement
    val scoreHtml = document.getElementById("score") as HTMLElement
    val startHtml = document.getElementById("start-message") as HTMLElement
    val gameOverHtml = document.getElementById("game-over") as HTMLElement

    window.setInterval({
        loop(pong, playgroundHtml, racketHtml, ballHtml, scoreHtml, gameOverHtml)
    }, pong.loopInterval)
    document.addEventListener("keydown", {
        val event = it as KeyboardEvent
        pong.pressedKeys.add(event.which)
        if (pong.isStopped()) {
            pong.status = Status.RUNNING
            startHtml.style.display = "none"
        }
    })
    document.addEventListener("keyup", {
        val event = it as KeyboardEvent
        pong.pressedKeys.remove(event.which)
    })
}

fun main() {
    println("starting pong game...")
    window.onload = { load(Pong()) }
}
