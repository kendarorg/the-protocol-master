package org.kendar.utils;

public class JsonMapperTypeInferrer {
    JsonMapper target = new JsonMapper();
    /*@Test
    void byteTest(){
        var result = target.toGenericContent(Base64.getEncoder().encodeToString(new byte[]{1,2,3,4,5,6}));
        assertTrue(result instanceof BinaryNode);
    }

    @Test
    void stringTEst(){
        var result = target.toGenericContent("Wetheaver77");
        assertTrue(result instanceof TextNode);
    }*/
}
