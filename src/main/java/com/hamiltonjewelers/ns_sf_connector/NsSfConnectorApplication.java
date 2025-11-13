package com.hamiltonjewelers.ns_sf_connector;

import com.hamiltonjewelers.ns_sf_connector.service.netsuite.NetsuiteAuthClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NsSfConnectorApplication implements CommandLineRunner {
    @Autowired
    private NetsuiteAuthClient netsuiteAuthClient;

    @Override
    public void run(String... args) {
        try {
            String token = netsuiteAuthClient.fetchAccessToken();

            System.out.println(token);
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