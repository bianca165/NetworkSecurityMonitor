package com.example.networksecuritymonitor.service;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class IPBlockerServiceUnitTest {

    private IPBlockerService service;

    private static final String FAKE_MONGO_URI = "mongodb://fake_user:fake_password@localhost:27017/test_db";

    @BeforeEach
    void setUp() {
        service = new IPBlockerService(FAKE_MONGO_URI);
    }

    @AfterEach
    void tearDown() {
        IPBlockerService.shutdownScheduler();
    }

    @Test
    void testWhitelistIPsConstant() throws Exception {
        Field whitelistField = IPBlockerService.class.getDeclaredField("WHITELIST_IPS");
        whitelistField.setAccessible(true);
        Set<String> whitelist = (Set<String>) whitelistField.get(null);
        assertTrue(whitelist.contains("35.241.129.42"));
    }

    @Test
    void testBlockedIPsInitiallyEmpty() throws Exception {
        Field blockedIPsField = IPBlockerService.class.getDeclaredField("blockedIPs");
        blockedIPsField.setAccessible(true);
        Set<String> blockedIPs = (Set<String>) blockedIPsField.get(service);
        assertTrue(blockedIPs.isEmpty());
    }

    @Test
    void testSchedulerIsSingleton() throws Exception {
        Field schedulerField = IPBlockerService.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Object scheduler = schedulerField.get(null);
        assertNotNull(scheduler);
    }
}