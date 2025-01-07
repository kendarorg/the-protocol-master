package org.kendar.di;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.di.simple.TestInterface;
import org.kendar.di.simple.list.ListUser;
import org.kendar.di.simple.reg.RegisteredItem;
import org.kendar.di.simple.reg.UsingRegistered;
import org.kendar.di.simple.tpls.*;

import static org.junit.jupiter.api.Assertions.*;

public class DiTest {
    private static DiService diService;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        diService = new DiService();
        diService.loadPackage("org.kendar");
    }

    @Test
    void testSimpleImplementation() {
        var result = diService.getInstance(TestInterface.class);
        assertNotNull(result);
    }

    @Test
    void testListImplementation() {
        var result = diService.getInstance(ListUser.class);
        assertNotNull(result);
        assertEquals(2, result.getItems().size());
    }

    @Test
    void testGenericImplementation() {
        var result = diService.getInstances(TemplateInterface.class);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testGenericSpecificParameterImplementation() {
        var result = diService.getInstance(UserOfStringTpl.class);
        assertNotNull(result);
        assertTrue(result.getTemplate() instanceof ImplOfString);
        var singleton = diService.getInstance(UserOfStringTpl.class);
        assertSame(singleton, result);

        var result2 = diService.getInstance(UserOfIntTpl.class);
        assertNotNull(result2);
        assertEquals(2, result2.getTemplate().size());
        assertEquals(2, result2.getTemplate().stream().filter(a -> a instanceof ImplOfInt).count());
        assertEquals(1, result2.getTemplate().stream().filter(a -> a instanceof ExtendedImplOfInt).count());

        var result3 = diService.getInstances(GenericUserOf.class);
        assertEquals(2, result3.size());
    }

    @Test
    void useRegistered() {
        RegisteredItem item = new RegisteredItem();
        diService.register(RegisteredItem.class, item);
        var res = diService.getInstance(RegisteredItem.class);
        assertSame(res, item);
        var using = diService.getInstance(UsingRegistered.class);
        assertSame(using.getItem(), res);
    }
}
