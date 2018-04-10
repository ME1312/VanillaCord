package uk.co.thinkofdeath.vanillacord;

import java.net.URL;
import java.net.URLClassLoader;

public class URLOverrideClassLoader extends ClassLoader {
    private ChildURLClassLoader childClassLoader;

    private static class FindClassClassLoader extends ClassLoader {
        public FindClassClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }
    }

    private static class ChildURLClassLoader extends URLClassLoader {
        private FindClassClassLoader realParent;

        public ChildURLClassLoader(URL[] urls, FindClassClassLoader realParent) {
            super(urls, null);
            this.realParent = realParent;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            try
            {
                // first try to use the URLClassLoader findClass
                return super.findClass(name);
            }
            catch( ClassNotFoundException e ) {
                // if that fails, we ask our real parent classloader to load the class (we give up)
                return realParent.loadClass(name);
            }
        }
    }

    public URLOverrideClassLoader(URL[] overrides) {
        super(Thread.currentThread().getContextClassLoader());

        childClassLoader = new ChildURLClassLoader(overrides, new FindClassClassLoader(this.getParent()));
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        try
        {
            // first we try to find a class inside the child classloader
            return childClassLoader.findClass(name);
        }
        catch( ClassNotFoundException e )
        {
            // didn't find it, try the parent
            return super.loadClass(name, resolve);
        }
    }
}