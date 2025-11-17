package com.example.networksecuritymonitor.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IPBlockerServiceIntegrationTest {

    @Autowired
    private IPBlockerService service;

    @BeforeEach
    void setUp() {
        //
    }

    @AfterEach
    void tearDown() {
        IPBlockerService.shutdownScheduler();
    }

    @Test
    void testSaveAndGetBlockedIP() {
        String testIp = "192.168.1.111";
        service.saveBlockedIPToDB(testIp);

        List<String> blocked = service.getBlockedIPsFromDB();
        assertTrue(blocked.contains(testIp), "IP-ul ar trebui să fie în baza de date");
    }

    @Test
    void testUpdateBlockDuration() {
        String testIp = "192.168.1.102";
        service.saveBlockedIPToDB(testIp);

        service.updateBlockDuration(testIp, 999);
    }

}
