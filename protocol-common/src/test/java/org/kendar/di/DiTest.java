package org.kendar.di;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kendar.di.simple.TestInterface;
import org.kendar.di.simple.list.ListOne;
import org.kendar.di.simple.list.ListUser;
import org.kendar.di.simple.named.NamedBase;
import org.kendar.di.simple.named.NamedDependency;
import org.kendar.di.simple.named.SimpleNamed;
import org.kendar.di.simple.named.SimpleUnnamed;
import org.kendar.di.simple.reg.RegisteredItem;
import org.kendar.di.simple.reg.UsingRegistered;
import org.kendar.di.simple.tpls.*;
import org.kendar.utils.Sleeper;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class DiTest {
    private static DiService diService;

    @BeforeEach
    void setUpBeforeEach() throws Exception {
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
        var listone = (ListOne) result.getItems().stream().filter(i -> i instanceof ListOne).findFirst().get();
        assertTrue(listone.isPostConstruct());
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

    @Test
    void namedWithString() {
        diService.registerNamed("test", "value");
        var res = diService.getInstance(SimpleUnnamed.class);
        assertEquals(res.getTest(), "value");
    }

    @Test
    void namedObject() {
        diService.registerNamed("test", "value");
        var res = diService.getInstance(NamedDependency.class);
        assertTrue(res.named instanceof SimpleNamed);
        assertEquals(res.named.getTest(), "value");
    }


    @Test
    void childContext() {
        diService.registerNamed("test", "value");
        var storage = new AtomicReference<Object>();
        var childContext = new AtomicReference<DiService>();
        new Thread(() -> {

            childContext.set(diService.createChildScope(TpmScopeType.THREAD));
            DiService.getThreadContext().registerNamed("test", "other");
            var instance = DiService.getThreadContext().getInstance(NamedDependency.class);
            storage.set(instance);
        }).start();
        Sleeper.sleep(100);
        var threadThing = (NamedDependency) storage.get();
        var outerInstance = diService.getInstance(NamedDependency.class);

        assertEquals(threadThing.named.getTest(), "other");
        assertEquals(outerInstance.named.getTest(), "value");
        DiService.threadsClean();
        assertThrows(RuntimeException.class, () -> childContext.get().getInstance(NamedDependency.class));
    }


    @Test
    void childContextNotOverriden() {
        diService.registerNamed("test", "value");
        var bases = diService.getInstances(NamedBase.class);
        var storage = new AtomicReference<Object>();
        new Thread(() -> {
            diService.createChildScope(TpmScopeType.THREAD);
            DiService.getThreadContext().registerNamed("test", "other");
            var instance = DiService.getThreadContext().getInstance(NamedDependency.class);
            storage.set(instance);
        }).start();
        Sleeper.sleep(100);
        var threadThing = (NamedDependency) storage.get();
        var outherInstance = diService.getInstance(NamedDependency.class);

        assertEquals("value", threadThing.named.getTest());
        assertEquals("value", outherInstance.named.getTest());
    }
}
