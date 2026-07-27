package com.hamiltonjewelers.ns_sf_connector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:connector;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"app.worker.enabled=false",
		"app.sync.customer.enabled=false",
		"netsuite.base_url=http://localhost",
		"netsuite.auth-url=http://localhost",
		"netsuite.client-id=test",
		"netsuite.cert-id=test",
		"salesforce.base_url=http://localhost",
		"salesforce.auth-url=http://localhost",
		"salesforce.account-url=http://localhost",
		"salesforce.client-id=test",
		"salesforce.client-secret=test"
})
class NsSfConnectorApplicationTests {

	@Test
	void contextLoads() {
	}

}
