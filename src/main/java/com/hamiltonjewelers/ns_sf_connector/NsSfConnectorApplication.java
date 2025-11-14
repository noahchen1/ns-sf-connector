package com.hamiltonjewelers.ns_sf_connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.TokenResponseDto;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.service.netsuite.authentication.NetsuiteAuthClient;
import com.hamiltonjewelers.ns_sf_connector.service.netsuite.customer.NetsuiteCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.service.sf.SfAuthClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class NsSfConnectorApplication implements CommandLineRunner {
    @Autowired
    private NetsuiteAuthClient netsuiteAuthClient;

    @Autowired
    private SfAuthClient sfAuthClient;

    @Autowired
    private NetsuiteCustomerClient netsuiteCustomerClient;

    @Override
    public void run(String... args) {
        try {
//            String tokenRes = netsuiteAuthClient.fetchAccessToken();
//            ObjectMapper mapper = new ObjectMapper();
//            TokenResponseDto parsedRes = mapper.readValue(tokenRes, TokenResponseDto.class);
//            String accessToken = parsedRes.getAccess_token();
//
//            List<CustomerItemDto> customers = netsuiteCustomerClient.getCustomers(accessToken);

            String tokenRes = sfAuthClient.fetchAccessToken();


            System.out.println(tokenRes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



	public static void main(String[] args) {
		SpringApplication.run(NsSfConnectorApplication.class, args);
	}
}


// ssh -i /d/repo/db.pem -L
// 15432:database-1.c8522k8ughqc.us-east-1.rds.amazonaws.com:5432
// ec2-user@54.197.108.8
// psql -h localhost -p 15432 -U postgres -d postgres