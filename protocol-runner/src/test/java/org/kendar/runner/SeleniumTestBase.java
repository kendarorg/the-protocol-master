package org.kendar.runner;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.TestInfo;
import org.kendar.tests.utils.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.FakeStrategy;
import org.testcontainers.containers.wait.strategy.PortWaitStrategy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SeleniumTestBase extends ApiTestBase {
    private static Path root;
    private static Path projectRoot;
    private static ComposeContainer environment;
    private static String tpmHost;
    private static HashMap<String, Integer> toWaitFor;
    private static final int defaultTimeout = 5000;
    private static final ConcurrentLinkedQueue logs = new ConcurrentLinkedQueue();
    private Path storage;
    private SeleniumIntegration selenium;
    private WebDriver driver;
    private String proxyHost;
    private Integer proxyPort;

    public static void tearDownAfterClassBase() {
        if (environment != null) {
            environment.stop();
        }
    }

    public static ComposeContainer getEnvironment() {
        return environment;
    }

    protected static Path getRoot() {
        return root;
    }

    protected static Path getProjectRoot() {
        return projectRoot;
    }

    protected static void setupDirectories(String project) throws Exception {
        root = Path.of("target").toAbsolutePath();
        projectRoot = Path.of(root.toString(), project);
        System.out.println(projectRoot);
    }

    protected static ComposeContainer setupContainer(String tpmHostExternal) throws Exception {
        tpmHost = tpmHostExternal;
        toWaitFor = new HashMap<>();
        environment = new ComposeContainer(
                Path.of(getProjectRoot().toString(), "docker-compose-testcontainers.yml").toFile()
        );

        toWaitFor.put(tpmHost, 8081);
        withExposedServiceHidden(tpmHost, 5005);
        withExposedService(tpmHost, 8081);
        withExposedService(tpmHost, 9000);
        withExposedService(tpmHost, 80);
        return environment;
    }

    protected static void startContainers() {
        for (var item : toWaitFor.entrySet()) {
            environment.withLogConsumer(item.getKey(), new Consumer<OutputFrame>() {
                @Override
                public void accept(OutputFrame outputFrame) {
                    var data = outputFrame.getUtf8StringWithoutLineEnding();
                    logs.add(data);
                    if (outputFrame.getType() == OutputFrame.OutputType.STDERR) {
                        System.err.println(data);
                    } else {
                        System.out.println(data);
                    }
                }
            });
        }
        environment.start();
        for (var item : toWaitFor.entrySet()) {
            waitPortAvailable(item.getKey(), item.getValue());
        }


        System.out.println("Containers started");
    }

    public static ComposeContainer withExposedService(String host, int ports) throws Exception {
        //environment.withExposedService(host, mainPort);
        environment.withExposedService(host, ports,
                new PortWaitStrategy().
                        forPorts(ports).
                        withStartupTimeout(Duration.ofSeconds(5)));

        toWaitFor.put(host, ports);
        return environment;
    }

    public static ComposeContainer withExposedServiceHidden(String host, int ports) throws Exception {
        //environment.withExposedService(host, mainPort);
        environment.withExposedService(host, ports,
                new FakeStrategy());
        toWaitFor.put(host, ports);
        return environment;
    }

    public static void waitPortAvailable(String service, int port) {
        TestSleeper.sleep(defaultTimeout, () -> {
            try {
                getEnvironment().getServiceHost(service, port);
                getEnvironment().getServicePort(service, port);
                return true;
            } catch (Exception e) {
                return false;
            }
        }, "Not started " + service + ":" + port);

    }

    public static void cleanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    cleanDirectory(file);
                file.delete();
            }
        }
    }

    protected CloseableHttpClient getHttpClient() {
        var custom = HttpClients.custom();
        var proxy = new HttpHost("http", proxyHost, proxyPort);
        var routePlanner = new DefaultProxyRoutePlanner(proxy);
        return custom.setRoutePlanner(routePlanner).build();
    }

    protected String httpGet(String path) {
        return new String(httpGetBinaryFile(path));
    }

    protected byte[] httpGetBinaryFile(String path) {
        try (var client = getHttpClient()) {
            var httpget = new HttpGet(path);
            var httpresponse = client.execute(httpget);

            var baos = new ByteArrayOutputStream();
            httpresponse.getEntity().writeTo(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void takeSnapshot() {
        selenium.takeSnapShot();
    }

    public void tearDownAfterEachBase() {
        Utils.setCache("driver", null);
        Utils.setCache("js", null);
        selenium.takeMessageSnapshot("End of test");
        if (driver != null) driver.quit();
        if (!Files.exists(storage)) {
            storage.toFile().mkdirs();
        }
        var alllogs = new StringBuffer();
        for (var log : logs.stream().map(s -> s + "\n").toList()) {
            alllogs.append(log);
        }
        try {
            Files.writeString(Path.of(storage.toString(), "docker.log"), alllogs);
        } catch (IOException e) {
            System.err.println("Error writing docker.log log file on path: " + storage);
        }
        driver = null;
    }

    protected void writeScenario() {
        var data = httpGetBinaryFile("http://localhost:5005/api/global/storage");
        if (!Files.exists(getStorage())) {
            getStorage().toFile().mkdirs();
        }
        try {
            Files.write(Path.of(getStorage().toString(), "scenario.zip"), data);
        } catch (IOException e) {
            System.err.println("Error writing docker.log log file on path: " + getStorage());
        }
    }

    protected Path getStorage() {
        return storage;
    }

    protected SeleniumIntegration getSelenium() {
        return selenium;
    }

    protected WebDriver getDriver() {
        return driver;
    }

    protected void beforeEachBase(TestInfo testInfo, int proxyPortPassed) throws Exception {
        logs.clear();
        if (testInfo != null && testInfo.getTestClass().isPresent() &&
                testInfo.getTestMethod().isPresent()) {
            var className = testInfo.getTestClass().get().getSimpleName();
            var method = testInfo.getTestMethod().get().getName();

            if (testInfo.getDisplayName().startsWith("[")) {
                var dsp = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9_\\-,.]", "_");
                storage = Path.of(getRoot().toString(), "target", "tests", className, method, dsp);
            } else {
                storage = Path.of(getRoot().toString(), "target", "tests", className, method);
            }
        }
        cleanDirectory(storage.toFile());
        Utils.getCache().clear();
        if (proxyPortPassed <= 0) {
            proxyHost = getEnvironment().getServiceHost(tpmHost, 9000);
            proxyPort = getEnvironment().getServicePort(tpmHost, 9000);
        } else {
            proxyHost = "127.0.0.1";
            proxyPort = proxyPortPassed;
        }


        selenium = new SeleniumIntegration(storage, proxyHost, proxyPort);
        selenium.takeMessageSnapshot("Start of test");
        selenium.resettingDriver();
        driver = Utils.getCache("driver");

        Utils.setCache("selenium", selenium);
        Utils.setCache("storage", storage);
        Utils.setCache("driver", driver);

        //Utils.killApacheLogger();
    }

    public void stopContainer(String host) throws Exception {
        var containerName = getEnvironment().getContainerByServiceName(host).get().getContainerInfo().getName().substring(1);
        var commandRunner = new CommandRunner(
                getProjectRoot(),
                "docker", "stop", containerName);
        commandRunner.run();
    }

    public void startContainer(String host) throws Exception {
        var containerName = getEnvironment().getContainerByServiceName(host).get().getContainerInfo().getName().substring(1);
        var commandRunner = new CommandRunner(
                getProjectRoot(),
                "docker", "start", containerName);
        commandRunner.run();
    }


    public boolean navigateTo(String url) {
        return getSelenium().navigateTo(url, true);
    }

    public boolean navigateTo(String url, boolean snapshot) {
        return getSelenium().navigateTo(url, snapshot);
    }

    public boolean navigateTo(String url, boolean snapshot,int timeout) {
        return getSelenium().navigateTo(url, snapshot);
    }

    public boolean check(BooleanSupplier supplier) {
        return check(defaultTimeout, supplier);
    }

    public boolean check(int timoutms, BooleanSupplier supplier) {
        return TestSleeper.sleepNoException(timoutms, supplier);
    }

    public String getPageSource() {
        return getDriver().getPageSource();
    }

    public boolean clickItem(String id) {
        return clickItem(defaultTimeout, id);
    }

    protected WebElement findElementById(String id) {
        return findElementById(defaultTimeout, id);
    }

    protected WebElement findElementByXPath(String id) {
        return findElementByXPath(defaultTimeout, id);
    }

    protected WebElement findElementByXPath(WebElement from, String id) {
        return findElementByXPath(defaultTimeout, from, id);
    }

    protected WebElement findElementByXPath(int timeoutms, WebElement from, String id) {
        var element = new ObjectContainer<WebElement>();
        TestSleeper.sleep(timeoutms, () -> {
            try {
                element.setObject(from.findElement(By.xpath(id)));
                return element.getObject() != null;
            } catch (Exception e) {
                return false;
            }
        });
        return element.getObject();
    }

    protected WebElement findElementByXPath(int timeoutms, String id) {
        var element = new ObjectContainer<WebElement>();
        TestSleeper.sleep(timeoutms, () -> {
            try {
                element.setObject(getDriver().findElement(By.xpath(id)));
                return element.getObject() != null;
            } catch (Exception e) {
                return false;
            }
        });
        return element.getObject();
    }

    protected List<WebElement> findElementsByXPath(String xpath) {
        return getDriver().findElements(By.xpath(xpath));
    }


    protected WebElement findElementById(int timeoutms, String id) {
        var element = new ObjectContainer<WebElement>();
        TestSleeper.sleep(timeoutms, () -> {
            try {
                element.setObject(getDriver().findElement(By.id(id)));
                return element.getObject() != null;
            } catch (Exception e) {
                return false;
            }
        });
        return element.getObject();
    }

    public boolean clickItem(int timeoutms, String id) {
        var element = findElementById(timeoutms, id);
        var done = new ObjectContainer<>(false);

        TestSleeper.sleep(timeoutms, () -> {
            try {
                element.click();
                done.setObject(true);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        if (!done.getObject()) {
            element.click();
            return true;
        } else {
            return true;
        }
    }

    protected void selectItem(String id, String value) {
        selectItem(defaultTimeout, id, value);
    }

    protected void selectItem(int timeoutms, String id, String value) {
        var element = findElementById(timeoutms, id);
        selectItem(element, value);
    }

    protected void selectItem(WebElement element, String value) {
        Select select = new Select(element);
        select.selectByValue(value);
    }

    public void fillItem(String id, String data) {
        fillItem(defaultTimeout, id, data);
    }

    public void fillItem(int timeoutms, String id, String data) {
        var element = findElementById(timeoutms, id);
        element.clear();
        element.sendKeys(data);
        getSelenium().takeSnapShot();
    }


    public void newTab(String id) {
        getSelenium().newTab(id);
        TestSleeper.sleep(200);
    }


    private String getCurrentTab() {
        return getSelenium().getCurrentTab();
    }

    public void switchToTab(String id) {
        getSelenium().switchToTab(id);
        TestSleeper.sleep(200);
    }

    public void executeScript(String script) {
        try {
            check(() -> !getPageSource().contains(script));
            ((JavascriptExecutor) getDriver()).executeScript(script);
            TestSleeper.sleep(200);
            takeSnapshot();
        } catch (Exception e) {
            System.out.println("Script execution failed");
        }
    }

    protected void alertWhenHumanDriven(String message) {

        selenium.takeMessageSnapshot(message);
        if (System.getenv("HUMAN_DRIVEN") != null) {
            ((JavascriptExecutor) getDriver()).executeScript("alert('" + message + "')");
            var alert = driver.switchTo().alert();
            while (alert != null) {
                try {
                    alert = driver.switchTo().alert();
                    TestSleeper.sleep(200);
                } catch (Exception e) {
                    alert = null;
                }
            }
            TestSleeper.sleep(1000);
        }
    }

    protected void cleanBrowserCache() {
        navigateTo("about:blank");
        getSelenium().clearStatus();
//        //driver.manage().deleteAllCookies();
//
//
//        var currentTab = getCurrentTab();
//        if(!existsTab("settings")) {
//            newTab("settings");
//        }else{
//            switchToTab("settings");
//        }
//        driver.get("chrome://settings/clearBrowserData");
//        driver.findElement(By.xpath("//settings-ui")).sendKeys(Keys.ENTER);
//        Sleeper.sleep(500);
//        navigateTo("about:blank");
//        switchToTab(currentTab);

    }

    private boolean existsTab(String id) {
        return getSelenium().existsTab(id);
    }


    public WebElement scrollFind(String id, long... extraLength) throws Exception {
        var js = (JavascriptExecutor) getDriver();
        var result = js.executeScript("return Math.max(" +
                "document.body.scrollHeight," +
                "document.body.offsetHeight," +
                "document.body.clientHeight," +
                "document.documentElement.scrollHeight," +
                "document.documentElement.offsetHeight," +
                "document.documentElement.clientHeight" +
                ");").toString();
        var length = Integer.parseInt(result);
        for (int i = 0; i < length; i += 100) {
            js.executeScript("window.scrollTo(0," + i + ")");
            var we = getDriver().findElements(By.id(id)).size();
            if (we == 0) {
                continue;
            }
            if (extraLength.length > 0) {
                js.executeScript("arguments[0].scrollIntoView(true);window.scrollBy(0,-100);", we);
                //js.executeScript("window.scrollTo(0," + (i+extraLength[0]) + ")");
            }
            return getDriver().findElement(By.id(id));
        }
        throw new RuntimeException("Unable to find item!");
    }

}
