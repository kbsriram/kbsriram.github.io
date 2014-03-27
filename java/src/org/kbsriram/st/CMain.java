package org.kbsriram.st;

import java.io.File;
import java.io.IOException;

public class CMain
{
    public final static void main(String args[])
        throws IOException
    {
        CFeed feed = new CFeed();

        File tplroot = new File(args[0]);
        File srcroot = new File(args[1]);
        File dstroot = new File(args[2]);

        walkRoot(feed, tplroot, srcroot, dstroot);
        feed.writeFeed(tplroot, dstroot);
    }

    private final static void walkRoot
        (CFeed feed, File tplroot, File srcroot, File dstroot)
        throws IOException
    {
        for (File child: srcroot.listFiles()) {
            if (child.isFile() && child.getName().endsWith(".txt")) {
                CText body = CText.parse(child);
                body.writeArticle(tplroot, dstroot);
                feed.addEntryFrom(body);
            }
            else if (child.isDirectory() && !child.getName().startsWith(".")) {
                walkRoot(feed, tplroot, child, dstroot);
            }
        }
    }
}
