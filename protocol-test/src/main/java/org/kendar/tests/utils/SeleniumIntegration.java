package org.kendar.tests.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.managers.ChromeDriverManager;
import io.github.bonigarcia.wdm.managers.ChromiumDriverManager;
import io.github.bonigarcia.wdm.versions.VersionDetector;
import org.apache.commons.io.FileUtils;
import org.kendar.tests.dm.TpmChromeDriverManager;
import org.kendar.tests.dm.TpmChromiumDriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"ThrowablePrintedToSystemOut", "ResultOfMethodCallIgnored"})
public class SeleniumIntegration {
    private final Path rootPath;
    private final String proxyHost;
    private final int proxyPort;
    private final Map<String, String> windowHandles = new HashMap<>();
    private WebDriver driver;
    private JavascriptExecutor js;
    private String currentTab;
    private int counter = 0;
    private Path browserPath;
    private boolean chromeNotChromium = false;

    public SeleniumIntegration(Path rootPath, String proxyHost, int proxyPort) {
        this.rootPath = rootPath;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public boolean navigateTo(String url) {
        return this.navigateTo(url, true);
    }

    public boolean navigateTo(String url, boolean snapshot) {
        var driver = (WebDriver) Utils.getCache("driver");
        var current = driver.getCurrentUrl();
        if (current.equalsIgnoreCase(url)) {
            TestSleeper.sleep(1000);
            if (!getCurrentTab().equals("settings")) {
                if (snapshot) takeSnapShot();
            }
            return true;
        }
        driver.get(url);
        if (!getCurrentTab().equals("settings")) {
            if (snapshot) takeSnapShot();
        }
        return false;
    }

    private void setupSize(WebDriver driver) {
        driver.manage().window().setSize(new Dimension(1366, 900));
    }

    public boolean isChrome() {
        return chromeNotChromium;
    }

    public boolean isChromium() {
        return !chromeNotChromium;
    }

    private String retrieveBrowserVersion() {
        Optional<Path> browserPath = WebDriverManager.chromedriver()
                .getBrowserPath();
        WebDriverManager webDriverManager = null;
        if (browserPath.isPresent()) {
            this.browserPath = browserPath.get();
            webDriverManager = ChromeDriverManager.getInstance();
            chromeNotChromium = true;
        } else {
            browserPath = WebDriverManager.chromiumdriver()
                    .getBrowserPath();
            if (browserPath.isPresent()) {
                this.browserPath = browserPath.get();
                webDriverManager = ChromiumDriverManager.getInstance();
                chromeNotChromium = false;
            } else {
                throw new RuntimeException("Chrome/chromium driver could not be setup");
            }
        }

        var config = webDriverManager.config();
        var versionDetector = new VersionDetector(config, null);
        var optionalVersion = versionDetector.getBrowserVersionFromTheShell(
                webDriverManager.getDriverManagerType().getBrowserNameLowerCase(), browserPath.get().toAbsolutePath().toString());

        if (optionalVersion.isPresent()) {
            var version = Integer.parseInt(optionalVersion.get());
            var available = webDriverManager.getDriverVersions().stream().
                    map(v -> Integer.parseInt(v.split("\\.")[0])).sorted().distinct().toList();
            var matching = available.get(available.size() - 1);
            if (available.stream().anyMatch(v -> v.equals(version))) {
                matching = version;
            }
            return matching.toString();
        } else {
            return null;
        }

    }

    public void seleniumInitialized() throws Exception {
        windowHandles.clear();
        //var chromeExecutable = SeleniumBase.findchrome();

        Proxy proxy = new Proxy();
        proxy.setHttpProxy(proxyHost + ":" + proxyPort);
        proxy.setSslProxy(proxyHost + ":" + proxyPort);
        proxy.setProxyType(Proxy.ProxyType.MANUAL);


        ChromeOptions capabilities = null;
        if (isChrome()) {
            capabilities = (ChromeOptions) new TpmChromeDriverManager().retrieveCapabilities();
        } else {
            capabilities = (ChromeOptions) new TpmChromiumDriverManager().retrieveCapabilities();
        }
        var version = retrieveBrowserVersion();
        if (version != null) {
            capabilities.setBrowserVersion(version);
        }

        capabilities.setAcceptInsecureCerts(true);
        capabilities.setBinary(browserPath.toFile());
        capabilities.setProxy(proxy);
        capabilities.setAcceptInsecureCerts(true);
        capabilities.addArguments("--remote-allow-origins=*");
        //options.addArguments("--disable-dev-shm-usage");
        //options.addArguments("disable-infobars"); // disabling infobars
        //options.addArguments("--disable-extensions"); // disabling extensions
        capabilities.addArguments("--disable-gpu"); // applicable to windows os only
        capabilities.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
        capabilities.addArguments("--no-sandbox"); // Bypass OS security model
        capabilities.addArguments("--disk-cache-size=0");//Disable cache
        if (System.getenv("HUMAN_DRIVEN") == null && System.getenv("RUN_VISIBLE") == null) {
            capabilities.addArguments("--headless");//Disable cache
        }


        if (isChrome()) {
            driver = WebDriverManager
                    .chromedriver()
                    .capabilities(capabilities)
                    .clearDriverCache()
                    .clearResolutionCache()
                    .create();
        } else {
            driver = WebDriverManager
                    .chromiumdriver()
                    .capabilities(capabilities)
                    .clearDriverCache()
                    .clearResolutionCache()
                    .create();
        }


        //driver.manage().deleteAllCookies();


        js = (JavascriptExecutor) driver;
        Utils.setCache("driver", driver);
        Utils.setCache("js", js);
        setupSize(driver);
        windowHandles.put("main", driver.getWindowHandle());
        currentTab = "main";


    }

    public void newTab(String id) {
        driver.switchTo().newWindow(WindowType.TAB);
        windowHandles.put(id, driver.getWindowHandle());
        currentTab = id;
    }

    public void switchToTab(String id) {
        driver.switchTo().window(windowHandles.get(id));
        currentTab = id;
    }

    public void resettingDriver() throws Exception {
        if (driver != null) driver.quit();
        Utils.setCache("driver", null);
        Utils.setCache("js", null);
        seleniumInitialized();
    }

    public void quitSelenium() throws Exception {
        driver.quit();
        Utils.setCache("driver", null);
        Utils.setCache("js", null);
        TestSleeper.sleep(1000);
        takeMessageSnapshot("End of test");
    }

    public void takeSnapShot() {

        try {
            if (driver.getCurrentUrl().startsWith("about:")) {
                return;
            }
            var dest = rootPath;
            if (!Files.exists(dest)) {
                rootPath.toFile().mkdirs();
            }
            counter++;
            TakesScreenshot scrShot = Utils.getCache("driver");
            File srcFile = scrShot.getScreenshotAs(OutputType.FILE);
            var destFilePath = Path.of(rootPath.toString(), "snap_" + String.format("%03d", counter) + ".png");
            File destFile = new File(destFilePath.toAbsolutePath().toString());
            FileUtils.copyFile(srcFile, destFile);
            Files.delete(srcFile.getAbsoluteFile().toPath());
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void takeMessageSnapshot(String text) {
        try {
            var dest = rootPath;
            if (!Files.exists(dest)) {
                rootPath.toFile().mkdirs();
            }

            counter++;
            var destFilePath = Path.of(rootPath.toString(), "snap_" + String.format("%03d", counter) + ".png");

            var img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            var g2d = img.createGraphics();
            var font = new Font("Arial", Font.PLAIN, 48);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            int width = 1024;//fm.stringWidth(text);
            int height = 768;//fm.getHeight();
            g2d.dispose();

            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            g2d = img.createGraphics();
            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, width, height);
            g2d.setFont(font);
            fm = g2d.getFontMetrics();
            g2d.setColor(Color.BLACK);
            g2d.drawString(text, 5, 5 + fm.getAscent());
            g2d.dispose();

            ImageIO.write(img, "png", new File(destFilePath.toAbsolutePath().toString()));
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public String getCurrentTab() {
        return currentTab;
    }

    public boolean existsTab(String id) {
        return windowHandles.containsKey(id);
    }

    public void clearStatus() {
        driver.manage().deleteAllCookies();
    }
}
