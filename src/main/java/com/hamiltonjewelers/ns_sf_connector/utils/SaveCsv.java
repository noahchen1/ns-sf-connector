package com.hamiltonjewelers.ns_sf_connector.utils;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.invLocation.NsInvLocationResponseDto;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SaveCsv {
    private SaveCsv() {}

    public static void saveInvLocationsToCsv(List<NsInvLocationResponseDto.InvLocation> invLocations) {
        try (FileWriter writer = new FileWriter("./inv_locations.csv")) {
            writer.append("Name,Item__c,Location_Id__c,Quantity_On_Hand__c").append("\n");

            for (NsInvLocationResponseDto.InvLocation invLocation : invLocations) {
                String name = invLocation.location() + "_" + invLocation.item();
                writer.append(name).append(",");
                writer.append(String.valueOf(invLocation.item())).append(",");
                writer.append(String.valueOf(invLocation.location())).append(",");
                writer.append(String.valueOf(invLocation.quantityOnHand())).append("\n");
            }
            System.out.println("CSV file 'inv_locations.csv' has been saved successfully.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
