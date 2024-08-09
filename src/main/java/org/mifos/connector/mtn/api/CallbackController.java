package org.mifos.connector.mtn.api;

import io.camunda.zeebe.client.ZeebeClient;
import lombok.extern.slf4j.Slf4j;
import org.mifos.connector.mtn.zeebe.ReceiveTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class CallbackController {

    @Autowired
    ZeebeClient zeebeClient;

    @Autowired
    ReceiveTask receiveTask;

    @PostMapping("/callback")
    public void receiveCallback(@RequestBody String callbackData) {
        log.info("Received callback");
        log.info("DTO is : {}", callbackData);
        receiveTask.publishTransactionCallback("abcd");
    }
}