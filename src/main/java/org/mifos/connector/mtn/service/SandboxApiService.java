package org.mifos.connector.mtn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;
import org.mifos.connector.mtn.data.Payer;
import org.mifos.connector.mtn.data.RequestToPayDTO;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

@Slf4j
@Component
public class SandboxApiService {
    String token_url = "https://sandbox.momodeveloper.mtn.com/collection/token/";
    String rtp_url = "https://sandbox.momodeveloper.mtn.com/collection/v1_0/requesttopay";
    String subscriptionKey = "";
    String username = "";
    String api_key = "";
    private static String getBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }

    public String generateAccessToken() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost postRequest = new HttpPost(token_url);
            postRequest.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", subscriptionKey));
            postRequest.setHeader(new BasicHeader("Authorization", getBasicAuthHeader(username, api_key)));

            CloseableHttpResponse response = httpClient.execute(postRequest);

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder responseString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseString.append(line);
            }

            JSONObject jsonResponse = new JSONObject(responseString.toString());
            String accessToken = jsonResponse.getString("access_token");

            log.info("Access Token is : {}", accessToken);

            return accessToken;
        } catch (Exception e) {
            log.info("An exception occurred : {}", e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public void requestToPay(String callbackUrl, String accessToken, String referenceId) throws
            JsonProcessingException, UnsupportedEncodingException {

        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost postRequest = new HttpPost(rtp_url);
            postRequest.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", subscriptionKey));
            postRequest.setHeader(new BasicHeader("X-Reference-Id", referenceId));
            postRequest.setHeader(new BasicHeader("X-Target-Environment", "sandbox"));
            postRequest.setHeader(new BasicHeader("X-Callback-Url", callbackUrl));
            postRequest.setHeader(new BasicHeader("Content-Type", "application/json"));
            postRequest.setHeader(new BasicHeader("Authorization", "Bearer " + accessToken));

            RequestToPayDTO requestToPayDTO = prepareDTO();
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(requestToPayDTO);

            StringEntity entity = new StringEntity(jsonPayload);
            postRequest.setEntity(entity);

            CloseableHttpResponse response = httpClient.execute(postRequest);
            log.info("Request to Pay Response Status: {}", response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            log.info("An exception occurred : {}", e.getMessage());
            e.printStackTrace();
        }

    }


    public void getTxnStatus(String callbackUrl, String accessToken, String referenceId) {
        String txnStatusUrl = "https://sandbox.momodeveloper.mtn.com/collection/v1_0/requesttopay/" + referenceId;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet getRequest = new HttpGet(txnStatusUrl);
            getRequest.setHeader(new BasicHeader("Ocp-Apim-Subscription-Key", subscriptionKey));
            getRequest.setHeader(new BasicHeader("X-Target-Environment", "sandbox"));
            getRequest.setHeader(new BasicHeader("Authorization", "Bearer " + accessToken));

            CloseableHttpResponse response = httpClient.execute(getRequest);

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder responseString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseString.append(line);
            }

            log.info("Transaction Status Response: {}", responseString.toString());
            JSONObject jsonResponse = new JSONObject(responseString.toString());




        } catch (Exception e) {
            log.info("An exception occurred : {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public RequestToPayDTO prepareDTO() {
        Payer payer = Payer.builder()
                .partyIdType("MSISDN")
                .partyId("23312345")
                .build();

        return RequestToPayDTO.builder()
                .amount("100")
                .currency("EUR")
                .externalId("123")
                .payer(payer)
                .build();
    }
}
