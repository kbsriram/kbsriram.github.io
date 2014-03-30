package org.kbsriram.multisign;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;

public class CMain
{
    // CMain pubkeyring secretkring root
    public static void main(String args[])
        throws Exception
    {
        ArmoredInputStream ain =
            new ArmoredInputStream
            (new BufferedInputStream
             (new FileInputStream(args[0])));
        PGPPublicKeyRing pkr = CPGPUtils.readPublicKeyRing(ain);
        ain.close();

        // First pick up siglist.
        List<File> tbds = new ArrayList<File>();
        walk(pkr.getPublicKey(), new File(args[2]), tbds);

        if (tbds.size() == 0) {
            // Nothing to do
            return;
        }

        // Okay - only now do we need to read the secretkey
        System.out.println("Needs signature: "+tbds);

        ain =
            new ArmoredInputStream
            (new BufferedInputStream
             (new FileInputStream(args[1])));
        char[] tmp = System.console().readPassword("password: ");
        PGPSecretKeyRing skr = CPGPUtils.readSecretKeyRing
            (ain, tmp);
        ain.close();
        PGPPrivateKey privkey = CPGPUtils.extractPrivateKey
            (skr.getSecretKey(), tmp);

        // nuke in-memory values too.
        for (int i=0; i<tmp.length; i++) { tmp[i] = '.'; }

        for (File tbd: tbds) {
            makeSignature
                (pkr.getPublicKey(), privkey, tbd,
                 new File(tbd.getPath()+".asc"));
        }
    }

    private final static void walk
        (PGPPublicKey pk, File root, List<File> tbds)
        throws Exception
    {
        File children[] = root.listFiles();
        for (int i=0; i<children.length; i++) {
            File child = children[i];
            if (child.getName().startsWith(".")) { continue; }
            else if (child.isDirectory()) { walk(pk, child, tbds); }
            else if (child.getName().endsWith(".html")) {
                maybeAddTbds(pk, child, tbds);
            }
            else if (child.getName().endsWith(".asc")) {
                // delete if no target exists
                maybeDeleteSig(child);
            }
        }
    }

    private final static void maybeDeleteSig(File sig)
    {
        String sigpath = sig.getPath();
        File check = new File
            (sigpath.substring(0, sigpath.length() - ".asc".length()));
        if (!check.canRead()) {
            System.out.println("delete: "+sig);
            sig.delete();
        }
    }

    private final static void maybeAddTbds
        (PGPPublicKey pk, File src, List<File> tbds)
        throws Exception
    {
        File sigfile = new File(src.getPath()+".asc");

        if (verifySignature(pk, src, sigfile)) {
            // System.out.println("ok: "+src);
            return;
        }
        tbds.add(src);
    }

    private final static void makeSignature
        (PGPPublicKey pk, PGPPrivateKey privkey, File src, File sig)
        throws Exception
    {
        
        FileInputStream srcin =
            (new FileInputStream(src));
        ArmoredOutputStream sigout =
            new ArmoredOutputStream
            (new BufferedOutputStream
             (new FileOutputStream(sig)));
        System.out.println("sign: "+src);
        CPGPUtils.writeDetachedSignature
            (privkey, pk.getAlgorithm(), srcin, sigout);
        srcin.close();
        sigout.close();
    }

    private final static boolean verifySignature
        (PGPPublicKey pk, File src, File sig)
        throws Exception
    {
        if (!sig.canRead()) { return false; }

        ArmoredInputStream sigin =
            new ArmoredInputStream
            (new BufferedInputStream
             (new FileInputStream(sig)));
        FileInputStream srcin =
            (new FileInputStream(src));

        boolean ret = CPGPUtils.verifyDetachedSignature(pk, srcin, sigin);
        srcin.close();
        sigin.close();
        return ret;
    }
}
