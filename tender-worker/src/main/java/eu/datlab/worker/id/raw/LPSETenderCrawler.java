package eu.datlab.worker.id.raw;

import eu.datlab.dataaccess.dao.DAOFactory;
import eu.dl.core.UnrecoverableException;
import eu.dl.dataaccess.dao.CrawlerAuditDAO;
import eu.dl.dataaccess.dao.RawDataDAO;
import eu.dl.dataaccess.dao.TransactionUtils;
import eu.dl.dataaccess.dto.raw.BasicCrawlerAuditRecord;
import eu.dl.dataaccess.dto.raw.CrawlerAuditRecord;
import eu.dl.dataaccess.dto.raw.RawData;
import eu.dl.worker.Message;
import eu.dl.worker.clean.utils.StringUtils;
import eu.dl.worker.clean.utils.URLSchemeType;
import eu.dl.worker.raw.BaseCrawler;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Tender crawler for Indonesia.
 */
public final class LPSETenderCrawler extends BaseCrawler {
    private static final String VERSION = "1.0";

    private final RawDataDAO<RawData> sourceDAO = DAOFactory.getDAOFactory().getRawTenderDAO(LPSEOrganizationDownloader.class.getName(),
        LPSEOrganizationDownloader.VERSION);

    private static final String MAIN_TAB_URL = "%1$s/eproc4/lelang/%2$s/pengumumanlelang";
    private static final String PESERTA_TAB_URL = "%1$s/eproc4/lelang/%2$s/peserta";
    private static final String HASIL_TAB_URL = "%1$s/eproc4/evaluasi/%2$s/hasil";
    private static final String PEMANANG_TAB_URL = "%1$s/eproc4/evaluasi/%2$s/pemenang";
    private static final String BERKONTRAK_TAB_URL = "%1$s/eproc4/evaluasi/%2$s/pemenangberkontrak";
    private static final String JADWAL_LINK_URL = "%1$s/eproc4/lelang/%2$s/jadwal";

    private static final String PAGE_URL = "%1$s/eproc4/lelang";

    private String currentSourceUrl;

    private final CrawlerAuditDAO<CrawlerAuditRecord> auditDAO = DAOFactory.getDAOFactory().getCrawlerAuditDAO(getName(), getVersion());


    private RemoteWebDriver webDriver;
    private WebDriverWait webDriverWait;
    private final JavascriptExecutor jsExecutor;

    private static final String LAST_PAGE_XPATH = "//li[@id='tbllelang_last']";
    private static final String LAST_PAGE_ID = "'tbllelang_last'";
    private static final String PREVIOUS_PAGE_XPATH = "//a[@aria-label='Previous']";
    private static final String PREVIOUS_PAGE_ID = "'tbllelang_previous'";
    private static final String NEXT_PAGE_XPATH = "//a[@aria-label='Next']";
    private static final String NEXT_PAGE_ID = "'tbllelang_next'";
    private static final String LAST_PAGE_ON_SOURCE_XPATH = "//a[@aria-controls='tbllelang' and @data-dt-idx='8']";

    private static final String ROWS_XPATH = "//*[@class='odd' or @class='even']";
    private static final String DATA_TABLES_INFO_XPATH = "//*[@class='dataTables_info']";
    private static final String CONTENT_XPATH = "//tbody";

    /**
     * Web driver initialization.
     */
    public LPSETenderCrawler() {
        super();
        webDriver = getWebDriver();
        webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(60));
        jsExecutor = webDriver;
    }

    /**
     * Initializes remote web driver with options.
     * @return web driver or null
     */
    private RemoteWebDriver getWebDriver(){
        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized");
        options.addArguments("disable_infobars");
        options.addArguments("--disable-gpu");
        options.setHeadless(true);
        options.setCapability(CapabilityType.BROWSER_NAME, "chrome");
        options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);

        try {
            return new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new UnrecoverableException("Unable to initialize web driver");
        }
    }

    @Override
    protected void doWork(final Message message) {
        String id = message.getValue("id");
        RawData raw = sourceDAO.getById(id);
        if (raw == null) {
            logger.error("Raw record {} doesn't exists", id);
            throw new UnrecoverableException("Raw record doesn't exist");
        }

        String sourceUrl = getBulletinUrl(raw);

        CrawlerAuditRecord audit = initCrawlerAuditRecord();

        // last crawled page for the  given organization
        Map<String, Integer> lastPages = (Map<String, Integer>) audit.getMetaData().get("lastCrawledPageNumbers");
        Integer pageNumber = lastPages.get(id);
        if (pageNumber != null) {
            logger.info("Last page number {} for organization {} found in crawler audit.", pageNumber, id);
        } else {
            pageNumber = 1;
        }
        int messagesCount = 0;

        // removes /eproc or /eproc4 and/or / from the end of url
        currentSourceUrl = sourceUrl.replaceAll("(/eproc4?)?/?$", "");

        String url = String.format(PAGE_URL, currentSourceUrl);

        logger.info("Crawling of {} starts", url);
        goToNthPage(pageNumber);

        List<WebElement> rows = webDriver.findElementsByXPath(ROWS_XPATH);
        while(rows != null && !rows.isEmpty()) {
            logger.info("Processing page {}# with url {}", pageNumber, String.format(PAGE_URL, currentSourceUrl));
            for (WebElement row : rows) {
                String tenderId = row.findElement(By.className("sorting_1")).getText();
                HashMap<String, Object> metaData = new HashMap<>();
                metaData.put("additionalUrls", Arrays.asList(
                        String.format(PESERTA_TAB_URL, currentSourceUrl, tenderId),
                        String.format(PEMANANG_TAB_URL, currentSourceUrl, tenderId),
                        String.format(HASIL_TAB_URL, currentSourceUrl, tenderId),
                        String.format(BERKONTRAK_TAB_URL, currentSourceUrl, tenderId),
                        String.format(JADWAL_LINK_URL, currentSourceUrl, tenderId)
                ));

                createAndPublishMessage(String.format(MAIN_TAB_URL, currentSourceUrl, tenderId), metaData);
                messagesCount += 1;
            }
            lastPages.put(id, pageNumber);
            auditDAO.save(audit);
            pageNumber += 1;
            if(!goToPreviousPage()) {
                break;
            }
            rows = webDriver.findElementsByXPath(ROWS_XPATH);
        }
        logger.info("Crawling for url {} successfully completed, {} messages sent", url, messagesCount);
    }

    /**
     * @param raw
     *      raw record
     * @return bulletin url
     * @throws UnrecoverableException
     *      in case that the url is not found in meta-data of raw record or is malformed
     */
    private String getBulletinUrl(final RawData raw) {
        URL sourceUrl;
        if (raw.getMetaData().containsKey("url")) {
            // attempts to clean URL
            sourceUrl = StringUtils.cleanURL((String) raw.getMetaData().get("url"), URLSchemeType.HTTP);
            if (sourceUrl == null) {
                logger.info("URL {} found in meta-data of raw record {} is malformed", raw.getId());
                throw new UnrecoverableException("URL is malformed");
            }
        } else {
            logger.info("URL not found in meta-data of raw record {}", raw.getId());
            throw new UnrecoverableException("URL not found");
        }

        return sourceUrl.toString();
    }

    /**
     * @return crawler audit record with initialized meta-data
     */
    private CrawlerAuditRecord initCrawlerAuditRecord() {
        CrawlerAuditRecord audit = auditDAO.getByNameAndVersion();
        if (audit == null) {
            audit = new BasicCrawlerAuditRecord();
        }
        if (audit.getMetaData() == null) {
            audit.setMetaData(new HashMap<>());
        }
        if (!audit.getMetaData().containsKey("lastCrawledPageNumbers")) {
            audit.getMetaData().put("lastCrawledPageNumbers", new HashMap<>());
        }

        return audit;
    }

    /**
     * Loads previous page by clicking on previous page button and waits until content is loaded.
     * @return false if current page is first, true otherwise
     */
    private boolean goToPreviousPage() {
        String pageInfo = webDriver.findElementByXPath(DATA_TABLES_INFO_XPATH).getText();
        if(pageInfo.contains(" 1 ")) {
            return false;
        }
        // click button via javascript executor because webdriver click fails for some pages
        // with exception 'ElementClickInterceptedException'
        jsExecutor.executeScript("document.getElementById(" + PREVIOUS_PAGE_ID + ").click();");
        // wait until content is loaded and page info is changed, it indicates that the new page was loaded
        webDriverWait.until(ExpectedConditions.and(
                ExpectedConditions.presenceOfElementLocated(By.xpath(CONTENT_XPATH)),
                ExpectedConditions.not(
                        ExpectedConditions.textToBe(By.xpath(DATA_TABLES_INFO_XPATH), pageInfo)),
                ExpectedConditions.presenceOfElementLocated(By.xpath(ROWS_XPATH))));
        return true;
    }

    /**
     * Loads next page by clicking on next page button and waits until content is loaded.
     */
    private void goToNextPage() {
        String pageInfo = webDriver.findElementByXPath(DATA_TABLES_INFO_XPATH).getText();
        // click button via javascript executor because webdriver click fails for some pages
        // with exception 'ElementClickInterceptedException'
        jsExecutor.executeScript("document.getElementById(" + NEXT_PAGE_ID + ").click();");
        // wait until content is loaded and page info is changed, it indicates that the new page was loaded
        webDriverWait.until(ExpectedConditions.and(
                ExpectedConditions.presenceOfElementLocated(By.xpath(CONTENT_XPATH)),
                ExpectedConditions.not(
                        ExpectedConditions.textToBe(By.xpath(DATA_TABLES_INFO_XPATH), pageInfo)),
                ExpectedConditions.presenceOfElementLocated(By.xpath(ROWS_XPATH))));
    }


    /**
     * Loads n-th page from the end and waits until content is loaded.
     * @param n number of required page (from the end)
     */
    private void goToNthPage(final int n) {
        String url = String.format(PAGE_URL, currentSourceUrl);
        try {
            webDriver.manage().window().maximize();
            webDriver.manage().timeouts().pageLoadTimeout(90, TimeUnit.SECONDS);
            webDriver.get(url);
            // wait until content is loaded properly and last page button is present
            webDriverWait.until(ExpectedConditions.and(
                    ExpectedConditions.presenceOfElementLocated(By.xpath(CONTENT_XPATH)),
                    ExpectedConditions.presenceOfElementLocated(By.xpath(LAST_PAGE_XPATH))));
        } catch (NoSuchSessionException e) {
            logger.info("No such session");
            webDriver = getWebDriver();
            webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(60));
            try {
                webDriver.manage().window().maximize();
                webDriver.manage().timeouts().pageLoadTimeout(90, TimeUnit.SECONDS);
                webDriver.get(url);
                // wait until content is loaded properly and last page button is present
                webDriverWait.until(ExpectedConditions.and(
                        ExpectedConditions.presenceOfElementLocated(By.xpath(CONTENT_XPATH)),
                        ExpectedConditions.presenceOfElementLocated(By.xpath(LAST_PAGE_XPATH))));
            } catch (NoSuchSessionException e1) {
                e.printStackTrace();
                throw new UnrecoverableException("No such session after reinitialising web driver");
            }
        } catch(Exception e) {
            currentSourceUrl = currentSourceUrl.replace("https://", "http://");
            String modifiedUrl = String.format(PAGE_URL, currentSourceUrl);
            logger.info("Unable to load page on url {}. Trying to load page on modified url {}", url, modifiedUrl);
            try {
                webDriver.get(modifiedUrl);
                // wait until content is loaded properly and last page button is present
                webDriverWait.until(ExpectedConditions.and(
                        ExpectedConditions.presenceOfElementLocated(By.xpath(CONTENT_XPATH)),
                        ExpectedConditions.presenceOfElementLocated(By.xpath(LAST_PAGE_XPATH))));
            } catch(WebDriverException wde) {
                logger.info("Unable to load page on url {}", modifiedUrl);
                throw new UnrecoverableException("Unable to load page", wde);
            }
        }

        boolean tooFewPagesAvailable = false;
        int lastPageOnSource = -1;
        try {
            lastPageOnSource = Integer.parseInt(webDriver.findElementByXPath(LAST_PAGE_ON_SOURCE_XPATH).getText());
        } catch (NoSuchElementException | NumberFormatException e) {
            // unable to get last page number from the list of page buttons, crawling will just start from the last page
            logger.info("Too few pages, search will just start from the last one.");
            tooFewPagesAvailable = true;
        }

        // if page number is grater than lastPageOnSource / 2 than start moving from the first page, otherwise start from the last page
        if(lastPageOnSource > 0 && n > lastPageOnSource / 2) {
            for(int i = 1; i < lastPageOnSource - n + 1; i++) {
                goToNextPage();
            }
        } else {
            String pageInfo = webDriver.findElementByXPath(DATA_TABLES_INFO_XPATH).getText();
            // if too few pages are available and the page info contains duplicated numbers,
            // then the first page is the only one page, so
            // we don't click on the last page button
            if (tooFewPagesAvailable
                    && Arrays.stream(pageInfo.split(" "))
                    .filter(a -> a.matches("\\d+"))
                    .collect(Collectors.toSet()).size() < 3) {
                return;
            }

            // click button via javascript executor because webdriver click fails for some pages
            // with exception 'ElementClickInterceptedException'
            jsExecutor.executeScript("document.getElementById(" + LAST_PAGE_ID + ").click();");

            // wait until content is loaded and page info is changed, it indicates that the new page was loaded
            webDriverWait.until(ExpectedConditions.and(
                    ExpectedConditions.presenceOfElementLocated(By.xpath(CONTENT_XPATH)),
                    ExpectedConditions.not(
                            ExpectedConditions.textToBe(By.xpath(DATA_TABLES_INFO_XPATH), pageInfo)),
                    ExpectedConditions.presenceOfElementLocated(By.xpath(ROWS_XPATH))));

            for(int i = 1; i < n; i++) {
                goToPreviousPage();
            }
        }
    }


    @Override
    protected TransactionUtils getTransactionUtils() {
        return DAOFactory.getDAOFactory().getTransactionUtils();
    }

    @Override
    protected String getVersion() {
        return VERSION;
    }
}
