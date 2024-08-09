package org.mifos.connector.mtn.zeebe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import lombok.extern.slf4j.Slf4j;
import org.mifos.connector.mtn.data.RequestToPayDTO;
import org.mifos.connector.mtn.data.RtpCallbackResponseDTO;
import org.mifos.connector.mtn.data.converter.ChannelToRtpCallback;
import org.mifos.connector.mtn.data.converter.ChannelToRtpConverter;
import org.mifos.connector.mtn.service.SendCallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class MtnWorker {
    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private SendCallbackService callbackService;

    @Autowired
    private ReceiveTask receiveTask;

    @Autowired
    private ChannelToRtpCallback rtpConverter;

    @Value("${payerIdentifier.callback.failure}")
    private String callbackFailure;

    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int workerMaxJobs;


    @PostConstruct
    public void setupWorkers() throws UnsupportedEncodingException, JsonProcessingException {
        workerExecuteRtpexecuteRtpTransfer();
        workerExecuteGetRtpStatus();
    }

    public void workerExecuteRtpexecuteRtpTransfer() {
        zeebeClient.newWorker().jobType("mtn-init-transfer").handler((client, job) -> {
            log.info("MTN RTP worker");
            log.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());

            Map<String, Object> variables = job.getVariablesAsMap();
            log.info("variables MTN : {}", variables);
            String requestBody = (String) variables.get("channelRequest");
            variables.put("testt", "abcd");

            executeRtpTransfer();
            client.newCompleteCommand(job.getKey()).variables(variables).send();

            String payerId = extractPayerIdentifier(requestBody);

            RtpCallbackResponseDTO dto = rtpConverter.convertToRtpCallbackResponseDTO(requestBody);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(dto);

            variables.put("response", jsonResponse);

            if (!payerId.equals(callbackFailure)) {
                callbackService.sendCallback(jsonResponse, "http://localhost:8080/callback");
            }

        }).name("mtn-init-transfer").maxJobsActive(workerMaxJobs).open();
    }


    public void workerExecuteGetRtpStatus() {
        zeebeClient.newWorker().jobType("get-mtn-transaction-status").handler((client, job) -> {

            log.info("MTN Get Status worker");
            log.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());

            Map<String, Object> variables = job.getVariablesAsMap();
            log.info("variables GST : {}", variables);
            executeGetStatus();
            client.newCompleteCommand(job.getKey()).send();

            receiveTask.publishTransactionCallback("abcd");

        }).name("get-mtn-transaction-status").maxJobsActive(workerMaxJobs).open();
    }

    private String extractPayerIdentifier(String requestBody) {

        ObjectMapper mapper = new ObjectMapper();
        String payerId = "";
        try {
            JsonNode root = mapper.readTree(requestBody);

            // Extract the payer partyIdentifier
            String payerPartyIdentifier = root.path("payer").path("partyIdInfo").path("partyIdentifier").asText();
            payerId = payerPartyIdentifier;

            log.info("Payer partyIdentifier: " + payerPartyIdentifier);
        } catch (Exception e) {
            log.error("Failed to parse channelRequest JSON", e);
        }

        return payerId;
    }

    private void executeRtpTransfer() throws UnsupportedEncodingException, JsonProcessingException {
        //Use API service
        log.info("executing RTP Transfer");
    }

    private void executeGetStatus() {
        // Use API service
        log.info("executing GST logic");
    }

}

