package eu.datlab.worker.cl.raw;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;
import eu.datlab.dataaccess.dao.DAOFactory;
import eu.dl.core.RecoverableException;
import eu.dl.core.UnrecoverableException;
import eu.dl.dataaccess.dao.RawDAO;
import eu.dl.dataaccess.dao.TransactionUtils;
import eu.dl.dataaccess.dto.raw.Raw;
import eu.dl.worker.Message;
import eu.dl.worker.raw.downloader.BaseDownloader;
import eu.dl.worker.utils.NetworkUtils;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Downloader for Chile.
 *
 * @param <T>
 */
public final class MPTenderDownloader<T extends Raw> extends BaseDownloader<T> {
    private static final String VERSION = "1";

    private static final String BASE_URL = "https://www.mercadopublico.cl";
    private static final String PAGE_INVALID = "procurement/Includes/images/robot.jpg";

    private WebClient webClient;

    /**
     * Create webclient for downloader.
     */
    public MPTenderDownloader() {
        getNewWebclient();

        // check whether TOR should be started
        if (config.getParam(getName() + ".torEnabled") != null
                && config.getParam(getName() + ".torEnabled").equals("1")) {
            NetworkUtils.enableTorForHttp();
        }
    }

    @Override
    public List<T> downloadAndPopulateRawData(final Message message) {
        final T rawData = rawDao.getEmptyInstance();

        final String sourceDataUrl = message.getValue("url");

        if (sourceDataUrl != null) {
            // check if url is no malformed
            try {
                rawData.setSourceUrl(new URL(sourceDataUrl));
            } catch (final MalformedURLException ex) {
                logger.error("Malformed URL {}", sourceDataUrl);
                throw new UnrecoverableException("Unable to download data because of malformed url", ex);
            }

            // download detail page
            HtmlPage detailPage;
            try {
                logger.info("Downloading detail page", sourceDataUrl);
                detailPage = webClient.getPage(sourceDataUrl);
                logger.info("Downloaded data from {}", sourceDataUrl);
            } catch (IOException e) {
                logger.error("Was not able to open detail page url: {}", e);
                throw new RecoverableException("Was not able to open detail page url: ", e);
            }

            if (detailPage.getWebResponse().getContentAsString().contains(PAGE_INVALID)) {
                throw new Error("We are blocked!");
            }

            // download metadata
            @SuppressWarnings("unchecked") final HashMap<String, Object> metadata = new HashMap();

            // download historial metadata
            try {
                // check if link exist and is not disabled
                final DomElement historialLink = detailPage.getElementById("imgHistorial");
                if (historialLink != null && !historialLink.getAttribute("disabled").equals("disabled")) {
                    // download the page
                    logger.info("Downloading historial page.");
                    final HtmlPage historialPage = webClient.getPage(BASE_URL + historialLink.getAttribute("href"));
                    logger.info("Historial page downloaded.");

                    // save data in metadata
                    @SuppressWarnings("unchecked") final HashMap<String, Object> historialData = new HashMap();
                    historialData.put("url", historialPage.getUrl().toString());
                    historialData.put("body", historialPage.getWebResponse().getContentAsString());
                    metadata.put("historial", historialData);
                } else {
                    logger.info("No historial page for this tender.");
                }
            } catch (IOException e) {
                logger.error("Error while downloading historial page: {}", e);
                throw new RecoverableException("Was not able to open historial page: ", e);
            }

            // download adjudicacion
            try {
                // check if link exist and is not disabled
                final DomElement adjudicacionLink = detailPage.getElementById("imgAdjudicacion");
                if (adjudicacionLink != null && !adjudicacionLink.getAttribute("disabled").equals("disabled")) {
                    // download the page
                    logger.info("Downloading adjudicacion page.");
                    HtmlPage adjudicacionPage = webClient.getPage(BASE_URL + adjudicacionLink.getAttribute("href"));
                    logger.info("Adjudicacion page downloaded.");

                    // save the page to temporary metadata
                    @SuppressWarnings("unchecked") final HashMap<String, Object> adjudicacionData = new HashMap();
                    adjudicacionData.put("url", adjudicacionPage.getUrl().toString());
                    adjudicacionData.put("body", adjudicacionPage.getWebResponse().getContentAsString());

                    // search for additional bodies to download
                    final List<HtmlAnchor> adjudicacionBodies = adjudicacionPage.getByXPath("//table[@id='grdItemOC']//tr/td/a");

                    // open and save bodies
                    final HashMap<String, String> adjudicacionBodyPages = new HashMap<>();
                    if (adjudicacionBodies != null && !adjudicacionBodies.isEmpty()) {
                        logger.info("Downloading bodies for adjudicacion from page.");
                        for (HtmlAnchor adjudicacionBody : adjudicacionBodies) {
                            boolean bidderValid;
                            int tries = 0;
                            do {
                                tries++;

                                if (detailPage == null) {
                                    try {
                                        logger.info("Downloading detail page", sourceDataUrl);
                                        detailPage = webClient.getPage(sourceDataUrl);
                                        logger.info("Downloaded data from {}", sourceDataUrl);
                                        adjudicacionPage = webClient.getPage(BASE_URL + adjudicacionLink.getAttribute("href"));
                                    } catch (IOException e) {
                                        logger.error("Was not able to open detail page url: {}", e);
                                        throw new RecoverableException("Was not able to open detail page url: ", e);
                                    }

                                    if (detailPage.getWebResponse().getContentAsString().contains(PAGE_INVALID)) {
                                        throw new Error("We are blocked!");
                                    }
                                }

                                HtmlPage adjudicacionBodyPage = (HtmlPage) adjudicacionPage.executeJavaScript(adjudicacionBody
                                        .getAttribute("onclick").replace("return false;", "")).getNewPage();
                                final String bidderName = ((HtmlSpan) adjudicacionBodyPage
                                        .getFirstByXPath("//span[@id='lblSocialReasonDesc']")).getTextContent();
                                bidderValid = adjudicacionBody.getTextContent().contains(bidderName);

                                if (!bidderValid) {
                                    detailPage = null;
                                    getNewWebclient();
                                } else {
                                    adjudicacionBodyPages.put(adjudicacionBody.getCanonicalXPath(), compress(adjudicacionBodyPage
                                            .getWebResponse().getContentAsString()));
                                }
                            } while (!bidderValid && tries < 20);

                            if (!bidderValid) {
                                logger.error("Unable to download correct bidders for this tender {}", sourceDataUrl);
                                throw new RecoverableException("Unable to download correct bidders for this tender " + sourceDataUrl);
                            }

                        }
                        logger.info("Bodies from adjudicacion page downloaded.");
                    }

                    // save historial page and additional bodies to final metadata
                    adjudicacionData.put("adjudicacionBodies", adjudicacionBodyPages);
                    metadata.put("adjudicacion", adjudicacionData);
                } else {
                    logger.info("No adjudicacion page for this tender.");
                }
            } catch (IOException e) {
                logger.error("Error while downloading adjudicacion page: {}", e);
                throw new RecoverableException("Was not able to open adjudicacion page: ", e);
            }

            rawData.setSourceData(detailPage.getWebResponse().getContentAsString());
            rawData.setSourceDataMimeType(detailPage.getWebResponse().getContentType());
            rawData.setMetaData(metadata);
        } else {
            logger.error("Invalid url, url NULL");
            throw new UnrecoverableException("Invalid url, url NULL");
        }

        // return result
        return Collections.singletonList(rawData);
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public RawDAO getRawDataDao() {
        return DAOFactory.getDAOFactory().getRawTenderDAO(getName(), getVersion());
    }

    @Override
    protected TransactionUtils getTransactionUtils() {
        return DAOFactory.getDAOFactory().getTransactionUtils();
    }

    /**
     * Comppress and encode (to be savable to db) long string.
     *
     * @param str string to compress
     * @return String
     * @throws IOException IOException
     */
    private static String compress(final String str) throws IOException {
        if (str == null || str.length() == 0) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();

        return Hex.encodeHexString(out.toByteArray());
    }

    @Override
    protected void postProcess(final T raw) {
    }

    /**
     * A.
     */
    private void getNewWebclient() {
        webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setTimeout(90000);
    }
}
