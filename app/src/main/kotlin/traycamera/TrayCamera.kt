package traycamera

import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.awt.AWTException
import java.awt.CheckboxMenuItem
import java.awt.Desktop
import java.awt.Font
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.MenuItem
import java.awt.Point
import java.awt.PopupMenu
import java.awt.Robot
import java.awt.SystemTray
import java.awt.TrayIcon
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Scanner
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

fun main() {
    TrayCamera().start()
}

class TrayCamera {
    companion object {
        const val MENU_BAR = "-"
        val snapshotsFolder = File(System.getProperty("user.home"), ".traycamera").apply { mkdir() }
        val logger: Logger = Logger.getLogger(TrayCamera::class.java.name)
        val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            .mapIndexed { index, screen ->
                Pair<GraphicsDevice, CheckboxMenuItem>(
                    screen,
                    CheckboxMenuItem("Screen ${index + 1}").apply { state = true }
                )
            }
    }

    fun start() {
        if (!SystemTray.isSupported()) {
            logger.warning("SystemTray is not supported")
            exitProcess(-1)
        }

        try {
            SystemTray.getSystemTray().add(trayIcon)
        } catch (e: AWTException) {
            logger.warning("TrayIcon could not be added.")
            exitProcess(-2)
        }
    }

    private val cameraOff = loadIconImageFromResource("/camera_off.png", "Camera")
    private val cameraOn = loadIconImageFromResource("/camera_on.png", "Camera")
    private val aboutMessage = loadTextFromResource("/about.txt")
    private var trayIcon = buildTrayIcon()

    private fun buildTrayIcon(): TrayIcon {
        val popup = PopupMenu().apply {
            add(MenuItem("Tray Camera (About)").apply { addActionListener { about() } })
            add(MENU_BAR)
            screens.forEach { (_, menuItem) -> add(menuItem) }
            add(MENU_BAR)
            add(MenuItem("Identify Screens").apply { addActionListener { identifyScreens() } })
            add(MenuItem("Open Image Folder").apply { addActionListener { openFolder() } })
            add(MENU_BAR)
            add(MenuItem("Exit").apply { addActionListener { System.exit(0) } })
        }
        return TrayIcon(cameraOff).apply { popupMenu = popup; addActionListener { cameraClick() } }
    }

    private fun cameraClick() {
        var actions = screens
            .filter { (_, menuItem) -> menuItem.state }
            .map { (screen, menuItem) -> { captureScreen("Screen${menuItem.label}", screen) } }
        if (actions.isNotEmpty()) {
            trayIcon.image = cameraOn
            SwingUtilities.invokeLater {
                actions.forEach { it() }
                generateQuickViewHtml()
                Thread.sleep(250)
                trayIcon.image = cameraOff
            }
        }
    }

    private fun captureScreen(name: String, screen: GraphicsDevice) {
        val capture = Robot().createScreenCapture(screen.defaultConfiguration.bounds)
        val timeStr = LocalDateTime.now().toString()
            .replace(":", "-")
            .replace(".", "-")
            .replace("T", "__")
            .subSequence(0, 20)
        val imageFile = File(snapshotsFolder, "${timeStr}__${name}.png")
        ImageIO.write(capture, "png", imageFile)
    }

    private fun generateQuickViewHtml() {
        val imageFiles = snapshotsFolder.listFiles().filter { it.name.endsWith(".png") && !it.isDirectory }
        val output = File(snapshotsFolder, "QuickView.html").apply { if (exists()) delete() }.printWriter()
        output.appendHTML().html {
            head {
                title("Tray Camera Quick View")
                comment("Generated Document - May be overwritten")
                style {
                    unsafe {
                        +"""|
                        |      body { background-color: lightblue; }
                        |      img { max-width:100%; height:auto; }
                        |    """.trimMargin()
                    }
                }
            }
            body {
                h1 { +"Tray Camera Quick View" }
                imageFiles.forEach {
                    h2 { +it.name }
                    p {
                        img { alt = it.name; src = it.name }
                    }
                }
            }
        }
        output.close()
    }

    private fun identifyScreens() {
        val frames = mutableListOf<JFrame>()
        val bigFont = Font("serif", Font.BOLD, 144)
        screens.forEachIndexed { index, (screen, _) ->
            val frame = JFrame("Screen Identifier", screen.defaultConfiguration).apply { isUndecorated = true }
            frame.contentPane.add(JLabel("Screen ${index + 1}").apply { font = bigFont })
            screen.defaultConfiguration.bounds.apply { frame.location = Point(x, y) }
            frames.add(frame.apply { pack(); isVisible = true })
        }
        SwingUtilities.invokeLater {
            Thread.sleep(1250)
            frames.forEach { it.isVisible = false; it.dispose() }
        }
    }

    private fun openFolder() {
        Desktop.getDesktop().open(snapshotsFolder)
    }

    private fun about() {
        JOptionPane.showMessageDialog(null, aboutMessage, "Tray Camera", JOptionPane.PLAIN_MESSAGE)
    }

    private fun loadTextFromResource(path: String): String {
        return Scanner(TrayCamera::class.java.getResourceAsStream(path), StandardCharsets.UTF_8)
            .useDelimiter("\\A").next()
    }

    private fun loadIconImageFromResource(path: String, description: String): Image {
        return ImageIcon(TrayCamera::class.java.getResource(path), description).image
    }
}