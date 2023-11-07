package de.b4sh.yart.extensions.terraform;

import com.hubspot.jinjava.objects.collections.SizeLimitingPyMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.leadpony.justify.internal.keyword.assertion.Assertion;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryObjectParserTest {

    private DictionaryObjectParser unit;

    @BeforeEach
    void setUp() {
        unit = new DictionaryObjectParser();
    }

    @AfterEach
    void tearDown() {
        unit = null;
    }

    @Test
    void testTransform() {
        SizeLimitingPyMap map = new SizeLimitingPyMap(generateTestMap(),10);
        String result =  DictionaryObjectParser.transform(map);

        String multiLineAssertionVal = "{\"veng_developer\" = {\n" +
                "    \"principal_object_id\" = \"43214321-1234-1234-1234-43214321\"\n" +
                "    \"app_role_id\" = \"12341234-1234-1234-1234-12341234\"\n" +
                "},\n" +
                "\"admin\" = {\n" +
                "    \"principal_object_id\" = \"43214321-1234-1234-1234-43214321\"\n" +
                "    \"app_role_id\" = \"12341234-1234-1234-1234-12341234\"\n" +
                "}}";

        Assertions.assertEquals(multiLineAssertionVal,result);
    }

    @Test
    void testTransformTestNull() {
        String result =  DictionaryObjectParser.transform(null);
        Assertions.assertEquals("", result);
    }

    private HashMap<String, Object> generateTestMap(){
        HashMap<String, Object> rootMap = new HashMap<>();
        HashMap<String, Object> first = new HashMap<>();
        HashMap<String, Object> second = new HashMap<>();

        first.put("app_role_id", "12341234-1234-1234-1234-12341234");
        first.put("principal_object_id", "43214321-1234-1234-1234-43214321");

        second.put("app_role_id", "12341234-1234-1234-1234-12341234");
        second.put("principal_object_id", "43214321-1234-1234-1234-43214321");

        rootMap.put("veng_developer",first);
        rootMap.put("admin", second);

        return rootMap;
    }
}