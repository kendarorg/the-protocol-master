package org.kendar.utils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classloader to allow loading correctly the classes loaded from the plugins into
 * the JTE templates.
 * This is the classloader used for the JTE templates. It is a URLClassloader to force it's usage
 * from JTE
 */
public class TPMPluginsClassLoader extends URLClassLoader {
    private final ClassLoader[] classLoaders;
    /**
     * Keep track of where the classes are
     */
    private final ConcurrentHashMap<String, ClassLoader> classLoaderMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     *
     * @param parent       The parent classloader, contains all definitions of existing class not
     *                     inside the plugins
     * @param urls         The Jars where the plugins exist (used for the JTE compilation)
     * @param classLoaders The classloader to load the -REAL- classes of the PL4J plugins here
     *                     are all the plugin-specific definitions
     */
    public TPMPluginsClassLoader(ClassLoader parent, List<URL> urls, ClassLoader... classLoaders) {
        super(urls.toArray(new URL[]{}), parent);
        this.classLoaders = classLoaders;
    }

    /**
     * Try to find the classes
     *
     * @param name    The <a href="#binary-name">binary name</a> of the class
     * @param resolve If {@code true} then resolve the class
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // If founded already the classloader for the given class, use it, to avoid conflicts
        if (classLoaderMap.containsKey(name)) {
            return classLoaderMap.get(name).loadClass(name);
        }
        Class<?> result = null;
        // Find a suitable classloader between the plugin ones
        for (ClassLoader classLoader : classLoaders) {
            try {
                result = classLoader.loadClass(name);
                classLoaderMap.put(name, classLoader);
                return result;
            } catch (Exception e) {

            }
        }
        try {
            // If nothing found search for the main classloader
            result = getParent().loadClass(name);
            classLoaderMap.put(name, this.getParent());
            return result;
        } catch (Exception e) {

        }
        // If nothing found go for the standard URLClassLoader
        return super.loadClass(name, resolve);

    }

    /**
     * Force searching through the class-loader siblings
     *
     * @param name the name of the class
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }
}