package org.kendar.utils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;

public class TPMPluginsClassLoader extends URLClassLoader {
    private final ClassLoader[] classLoaders;

    public TPMPluginsClassLoader(ClassLoader parent, List<URL> urls, ClassLoader... classLoaders) {
        super(urls.toArray(new URL[]{}), parent);
        this.classLoaders = classLoaders;
    }

    private HashMap<String,ClassLoader> classLoaderMap = new HashMap<>();

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(classLoaderMap.containsKey(name)){
            return classLoaderMap.get(name).loadClass(name);
        }
        Class<?> result = null;
        for(ClassLoader classLoader : classLoaders){
            try {
                result= classLoader.loadClass(name);
                classLoaderMap.put(name, classLoader);
                return result;
            }catch (Exception e){

            }
        }
        try {
            result = getParent().loadClass(name);
            classLoaderMap.put(name, this.getParent());
            return result;
        }catch(Exception e){

        }
        return super.loadClass(name,resolve);

    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }
/*
    @Override
    protected Package definePackage(String name, Manifest man, URL url) {
        throw new RuntimeException();
    }

    @Override
    public URL findResource(String name) {
        throw new RuntimeException();
    }

    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        throw new RuntimeException();
    }

    @Override
    public Enumeration<URL> findResources(String name) {
        throw new RuntimeException();
    }

    @Override
    protected void addURL(URL url) {
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        throw new RuntimeException();
    }

    @Override
    public void close() throws IOException {

    }

    public URL[] getURLs() {
        return new URL[0];
    }

    @Override
    protected Class<?> findClass(String moduleName,
                                 String name) {
        throw new RuntimeException();
    }*/
}