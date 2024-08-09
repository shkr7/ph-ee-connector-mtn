package org.mifos.connector.mtn;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.UnsupportedEncodingException;

@SpringBootApplication
public class MtnConnectorApplication {

	public static void main(String[] args) {
		SpringApplication.run(MtnConnectorApplication.class, args);
	}

}
