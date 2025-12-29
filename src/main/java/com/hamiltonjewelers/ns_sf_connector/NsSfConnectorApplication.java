package com.hamiltonjewelers.ns_sf_connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.item.invLocation.NsInvLocationClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.invLocation.SfInvLocationBulkClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.invLocation.SfInvLocationClient;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.auth.NsAuthResponseDto;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.invLocation.NsInvLocationResponseDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.auth.SfAuthResponseDto;
import com.hamiltonjewelers.ns_sf_connector.utils.SaveCsv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class NsSfConnectorApplication {
    @Autowired
    private NsAuthClient nsAuthClient;

    @Autowired
    private SfAuthClient sfAuthClient;

    @Autowired
    private NsCustomerClient nsCustomerClient;

    @Autowired
    private SfAccountClient sfAccountClient;

    @Autowired
    private NsInvLocationClient nsInvLocationClient;

    @Autowired
    private SfInvLocationClient sfInvLocationClient;

    @Autowired
    private SfInvLocationBulkClient sfInvLocationBulkClient;

//    @Override
//    public void run(String... args) {
//        try {
//            String accessToken = nsAuthClient.fetchAccessToken();
//
//            System.out.println(accessToken);
//
//            String sfToken = sfAuthClient.fetchAccessToken();
//
//            System.out.println(sfToken);
//
//            List<NsInvLocationResponseDto.InvLocation> res = nsInvLocationClient.getInvLocation(accessToken);
//
//            SaveCsv.saveInvLocationsToCsv(res);

//            String jobId = sfInvLocationBulkClient.createJob(sfToken);
//
//            System.out.println(jobId);
//
//            sfInvLocationBulkClient.uploadCsv(sfToken, jobId, Path.of("./inv_locations.csv"));
//
//            sfInvLocationBulkClient.closeJob(sfToken, jobId);
//
//            sfInvLocationBulkClient.waitForCompletion(sfToken, jobId);
//
//            String failed = sfInvLocationBulkClient.getFailedResults(sfToken, jobId);
//            System.out.println("FAILED ROWS:\n" + failed);


//            Map<String, Object> invLocationFields = Map.of(
//                    "Name", "Test Inv Location",
//                    "Item__c", "Test Item",
//                    "Location_Id__c", "28",
//                    "Quantity_On_Hand__c", 1
//            );
//
//
//            String res = sfInvLocationClient.createInvLocation(sfToken, invLocationFields);
//
//            System.out.println(res);

//            {"id":"a0wV90000011AETIA2","success":true,"errors":[]}



//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }


    public static void main(String[] args) {
        SpringApplication.run(NsSfConnectorApplication.class, args);
    }
}

// ssh -i /d/repo/db.pem -L
// 15432:database-1.c8522k8ughqc.us-east-1.rds.amazonaws.com:5432
// ec2-user@54.197.108.8
// psql -h localhost -p 15432 -U postgres -d postgres