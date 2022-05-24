package uk.co.thinkofdeath.vanillacord.library;

import java.net.URL;
import java.net.URLClassLoader;

public final class PatchLoader extends ClassLoader {
    private final Reloaded child;

    public PatchLoader(URL[] urls) {
        super(Thread.currentThread().getContextClassLoader());
        child = new Reloaded(urls, new Wrapped(this.getParent()));
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            // Search the child classloaders
            return child.findClass(name);
        } catch(ClassNotFoundException e) {
            // Fallback to the parent classloader
            return super.loadClass(name, resolve);
        }
    }

    private static final class Reloaded extends URLClassLoader {
        private final Wrapped next;

        private Reloaded(URL[] urls, Wrapped next) {
            super(urls, null);
            this.next = next;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                // First, try to load from the URLClassLoader
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                // If that fails, we ask our real parent classloader to load the class (we give up)
                return next.loadClass(name);
            }
        }
    }

    private static final class Wrapped extends ClassLoader {
        private Wrapped(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }
    }
}