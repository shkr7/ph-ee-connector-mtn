package org.mifos.connector.mtn.service;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SendCallbackService {

    private static final Logger logger = LoggerFactory.getLogger(SendCallbackService.class);
    private static final int TIMEOUT = 5000;

    public void sendCallback(String body, String callbackURL) {
        logger.info("Sending callback to URL: {}", callbackURL);
        logger.info("Request body: {}", body);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(callbackURL);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(body));

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT)
                    .setSocketTimeout(TIMEOUT)
                    .build();
            httpPost.setConfig(requestConfig);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int responseCode = response.getStatusLine().getStatusCode();

                logger.info("Response code: {}", responseCode);
            }
        } catch (Exception e) {
            logger.error("Error sending callback", e);
        }
    }
}