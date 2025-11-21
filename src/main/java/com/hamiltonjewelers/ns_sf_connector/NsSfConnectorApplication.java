package com.hamiltonjewelers.ns_sf_connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.auth.NsAuthResponseDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.auth.SfAuthResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NsSfConnectorApplication implements CommandLineRunner {
    @Autowired
    private NsAuthClient nsAuthClient;

    @Autowired
    private SfAuthClient sfAuthClient;

    @Autowired
    private NsCustomerClient nsCustomerClient;

    @Autowired
    private SfAccountClient sfAccountClient;

    @Override
    public void run(String... args) {
        try {
            String tokenRes = nsAuthClient.fetchAccessToken();
            ObjectMapper mapper = new ObjectMapper();
            NsAuthResponseDto parsedRes = mapper.readValue(tokenRes, NsAuthResponseDto.class);
            String accessToken = parsedRes.getAccess_token();

            System.out.println(accessToken);

            String sfTokenRes = sfAuthClient.fetchAccessToken();

            ObjectMapper sfMapper = new ObjectMapper();
            SfAuthResponseDto parsedSfRes = sfMapper.readValue(sfTokenRes, SfAuthResponseDto.class);
            String sfToken = parsedSfRes.getAccess_token();
            System.out.println(sfToken);
            String accountRes = sfAccountClient.getAccounts(sfToken);

            System.out.println(accountRes);
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