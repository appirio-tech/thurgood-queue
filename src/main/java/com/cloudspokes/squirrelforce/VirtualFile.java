package com.cloudspokes.squirrelforce;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;

/**
 * Jetty specific implementation of a virtual file system (VFS) abstracting the
 * contents of the standard webapp directory.
 */
public class VirtualFile {

    /**
     * @param relativePath relative path of a desired resource in the VFS
     * @return the actual {@link File} entry corresponding to the relative path (may be a directory)
     */
    public static File fromRelativePath(String relativePath) {
        Context context = ContextHandler.getCurrentContext();
        ContextHandler contextHandler = context.getContextHandler();
        try {
            Resource resource = contextHandler.getResource(URIUtil.SLASH + relativePath);
            return resource.getFile();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}