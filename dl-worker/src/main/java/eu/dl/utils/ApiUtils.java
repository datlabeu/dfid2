package eu.dl.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.dl.core.UnrecoverableException;
import eu.dl.worker.raw.utils.DownloaderUtils;
import eu.dl.worker.utils.ThreadUtils;
import org.jsoup.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.jsoup.Connection.Method.POST;

/**
 * Supporting class for API queries.
 *
 * @author Tomas Mrazek
 */
public class ApiUtils {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    protected final ObjectMapper mapper;

    protected final String baseUrl;
    protected final String user;
    protected final String password;

    /**
     * How many times attempts to retrieve an api response when the request fails on time out exception.
     */
    protected static final int TIMEOUT_ATTEMPTS = 5;
    protected static final int TIMEOUT_ATTEMPTS_SLEEP = 60000;

    /**
     * Default constructor for api secured by password.
     *
     * @param baseUrl
     *      API base url
     * @param user
     *      user name
     * @param password
     *      user password
     */
    public ApiUtils(final String baseUrl, final String user, final String password) {
        this.baseUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/");
        this.user = user;
        this.password = password;

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Default constructor.
     *
     * @param baseUrl
     *      API base url
     */
    public ApiUtils(final String baseUrl) {
        this(baseUrl, null, null);
    }

    /**
     * Performs API request and returns response processed by handler.
     *
     * @param method
     *      HTTP method {@link Connection.Method}
     * @param endpoint
     *      API endpoint
     * @param headers
     *      request headers
     * @param data
     *      request payload
     * @param handler
     *      response handler, method accepts two parameters, response and json mapper.
     * @param <T>
     *      response handler output class
     * @return response
     */
    public final <T> T request(final Connection.Method method, final String endpoint, final Map<String, String> headers,
                         final Map<String, String> data, final BiFunction<Connection.Response, ObjectMapper, T> handler) {
        String url = baseUrl + (endpoint.startsWith("/") ? endpoint.substring(1) : endpoint);
        Connection.Response response = request(url, method, headers, data, 1);
        return handler.apply(response, mapper);
    }

    /**
     * Executes HTTP request with an URL and returns a response. If the request fails on time out exception it attempts up to 5 times
     * retrieve a response.
     *
     * @param url
     *      requested URL
     * @param method
     *      request method, if NULL the GET method is used
     * @param headers
     *      request headers
     * @param data
     *      request payload
     * @param attempt
     *      number of attempt
     * @return response
     */
    protected final Connection.Response request(final String url, final Connection.Method method, final Map<String, String> headers,
                                        final Map<String, String> data, final int attempt) {
        if (attempt > TIMEOUT_ATTEMPTS) {
            logger.error("Read from {} timed out. Limit {} of attempts was reached.", url, TIMEOUT_ATTEMPTS);
            throw new UnrecoverableException("Read timed out");
        }

        Connection.Response response = DownloaderUtils.getUrlResponse(url, method, headers, data);
        if (response.statusCode() == 503) {
            logger.warn("Server is unavailable. Attempt number {}.", url, attempt);
            ThreadUtils.sleep(TIMEOUT_ATTEMPTS_SLEEP);
            return request(url, method, headers, data, attempt + 1);
        }
        return  response;
    }

    /**
     * Performs API request and returns response.
     *
     * @param method
     *      HTTP method {@link Connection.Method}
     * @param endpoint
     *      API endpoint
     * @param headers
     *      request headers
     * @param data
     *      request payload
     * @return response
     */
    public final Connection.Response request(final Connection.Method method, final String endpoint, final Map<String, String> headers,
                                       final Map<String, String> data) {
        return this.request(method, endpoint, headers, data, (r, m) -> r);
    }

    /**
     * Performs API request on secured endpoint.
     *
     * @param method
     *      HTTP method {@link Connection.Method}
     * @param endpoint
     *      API endpoint
     * @param headers
     *      request headers
     * @param data
     *      request payload
     * @param token
     *      authentication token
     * @param handler
     *      response handler, method accepts two parameters, response and json mapper.
     * @param <T>
     *      response handler output class
     * @return response
     */
    public final <T> T secured(final Connection.Method method, final String endpoint, final Map<String, String> headers,
                         final Map<String, String> data, final String token,
                         final BiFunction<Connection.Response, ObjectMapper, T> handler) {
        Map<String, String> allData = new HashMap<>();
        allData.put("auth_token", token);
        if (data != null) {
            allData.putAll(data);
        }

        return request(method, endpoint, headers, allData, handler);
    }

    /**
     * @return API authentication
     */
    public final String getAuthToken() {
        Map<String, String> data = new HashMap<>();
        data.put("username", user);
        data.put("password", password);

        return request(POST, "login", null, data,
            (r, m) -> {
                if (r.statusCode() == 200) {
                    try {
                        JsonNode json = m.readTree(r.body());
                        return json.path("auth_token").asText(null);
                    } catch(IOException ex) {
                        logger.error("Unable to parse API authentication token because of ", ex);
                        throw new UnrecoverableException("Unable to parse API authentication token", ex);
                    }
                } else {
                    logger.error("Unable to get API authentication token because of {}", r.body());
                    throw new UnrecoverableException("Unable to get API authentication token");
                }
            });
    }

    /**
     * @return object mapper
     */
    public final ObjectMapper getMapper() {
        return mapper;
    }
}
