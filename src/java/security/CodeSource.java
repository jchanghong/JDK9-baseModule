/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.security;


import java.net.URL;
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.*;
import sun.net.util.URLUtil;



public class CodeSource implements java.io.Serializable {

    private static final long serialVersionUID = 4977541819976013951L;


    private URL location;

    /*
     * The code signers.
     */
    private transient CodeSigner[] signers = null;

    /*
     * The code signers. Certificate chains are concatenated.
     */
    private transient java.security.cert.Certificate[] certs = null;

    // cached SocketPermission used for matchLocation
    private transient SocketPermission sp;

    // for generating cert paths
    private transient CertificateFactory factory = null;


    private transient String locationNoFragString;


    public CodeSource(URL url, java.security.cert.Certificate[] certs) {
        this.location = url;
        if (url != null) {
            this.locationNoFragString = URLUtil.urlNoFragString(url);
        }

        // Copy the supplied certs
        if (certs != null) {
            this.certs = certs.clone();
        }
    }


    public CodeSource(URL url, CodeSigner[] signers) {
        this.location = url;
        if (url != null) {
            this.locationNoFragString = URLUtil.urlNoFragString(url);
        }

        // Copy the supplied signers
        if (signers != null) {
            this.signers = signers.clone();
        }
    }


    @Override
    public int hashCode() {
        if (location != null)
            return location.hashCode();
        else
            return 0;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        // objects types must be equal
        if (!(obj instanceof CodeSource))
            return false;

        CodeSource cs = (CodeSource) obj;

        // URLs must match
        if (location == null) {
            // if location is null, then cs.location must be null as well
            if (cs.location != null) return false;
        } else {
            // if location is not null, then it must equal cs.location
            if (!location.equals(cs.location)) return false;
        }

        // certs must match
        return matchCerts(cs, true);
    }


    public final URL getLocation() {
        /* since URL is practically immutable, returning itself is not
           a security problem */
        return this.location;
    }


    String getLocationNoFragString() {
        return locationNoFragString;
    }


    public final java.security.cert.Certificate[] getCertificates() {
        if (certs != null) {
            return certs.clone();

        } else if (signers != null) {
            // Convert the code signers to certs
            ArrayList<java.security.cert.Certificate> certChains =
                        new ArrayList<>();
            for (int i = 0; i < signers.length; i++) {
                certChains.addAll(
                    signers[i].getSignerCertPath().getCertificates());
            }
            certs = certChains.toArray(
                        new java.security.cert.Certificate[certChains.size()]);
            return certs.clone();

        } else {
            return null;
        }
    }


    public final CodeSigner[] getCodeSigners() {
        if (signers != null) {
            return signers.clone();

        } else if (certs != null) {
            // Convert the certs to code signers
            signers = convertCertArrayToSignerArray(certs);
            return signers.clone();

        } else {
            return null;
        }
    }


    public boolean implies(CodeSource codesource)
    {
        if (codesource == null)
            return false;

        return matchCerts(codesource, false) && matchLocation(codesource);
    }


    boolean matchCerts(CodeSource that, boolean strict)
    {
        boolean match;

        // match any key
        if (certs == null && signers == null) {
            if (strict) {
                return (that.certs == null && that.signers == null);
            } else {
                return true;
            }
        // both have signers
        } else if (signers != null && that.signers != null) {
            if (strict && signers.length != that.signers.length) {
                return false;
            }
            for (int i = 0; i < signers.length; i++) {
                match = false;
                for (int j = 0; j < that.signers.length; j++) {
                    if (signers[i].equals(that.signers[j])) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }
            return true;

        // both have certs
        } else if (certs != null && that.certs != null) {
            if (strict && certs.length != that.certs.length) {
                return false;
            }
            for (int i = 0; i < certs.length; i++) {
                match = false;
                for (int j = 0; j < that.certs.length; j++) {
                    if (certs[i].equals(that.certs[j])) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }
            return true;
        }

        return false;
    }



    private boolean matchLocation(CodeSource that) {
        if (location == null)
            return true;

        if ((that == null) || (that.location == null))
            return false;

        if (location.equals(that.location))
            return true;

        if (!location.getProtocol().equalsIgnoreCase(that.location.getProtocol()))
            return false;

        int thisPort = location.getPort();
        if (thisPort != -1) {
            int thatPort = that.location.getPort();
            int port = thatPort != -1 ? thatPort
                                      : that.location.getDefaultPort();
            if (thisPort != port)
                return false;
        }

        if (location.getFile().endsWith("/-")) {
            // Matches the directory and (recursively) all files
            // and subdirectories contained in that directory.
            // For example, "/a/b/-" implies anything that starts with
            // "/a/b/"
            String thisPath = location.getFile().substring(0,
                                            location.getFile().length()-1);
            if (!that.location.getFile().startsWith(thisPath))
                return false;
        } else if (location.getFile().endsWith("/*")) {
            // Matches the directory and all the files contained in that
            // directory.
            // For example, "/a/b/*" implies anything that starts with
            // "/a/b/" but has no further slashes
            int last = that.location.getFile().lastIndexOf('/');
            if (last == -1)
                return false;
            String thisPath = location.getFile().substring(0,
                                            location.getFile().length()-1);
            String thatPath = that.location.getFile().substring(0, last+1);
            if (!thatPath.equals(thisPath))
                return false;
        } else {
            // Exact matches only.
            // For example, "/a/b" and "/a/b/" both imply "/a/b/"
            if ((!that.location.getFile().equals(location.getFile()))
                && (!that.location.getFile().equals(location.getFile()+"/"))) {
                return false;
            }
        }

        if (location.getRef() != null
            && !location.getRef().equals(that.location.getRef())) {
            return false;
        }

        String thisHost = location.getHost();
        String thatHost = that.location.getHost();
        if (thisHost != null) {
            if (("".equals(thisHost) || "localhost".equals(thisHost)) &&
                ("".equals(thatHost) || "localhost".equals(thatHost))) {
                // ok
            } else if (!thisHost.equals(thatHost)) {
                if (thatHost == null) {
                    return false;
                }
                if (this.sp == null) {
                    this.sp = new SocketPermission(thisHost, "resolve");
                }
                if (that.sp == null) {
                    that.sp = new SocketPermission(thatHost, "resolve");
                }
                if (!this.sp.implies(that.sp)) {
                    return false;
                }
            }
        }
        // everything matches
        return true;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(this.location);

        if (this.certs != null && this.certs.length > 0) {
            for (int i = 0; i < this.certs.length; i++) {
                sb.append( " " + this.certs[i]);
            }

        } else if (this.signers != null && this.signers.length > 0) {
            for (int i = 0; i < this.signers.length; i++) {
                sb.append( " " + this.signers[i]);
            }
        } else {
            sb.append(" <no signer certificates>");
        }
        sb.append(")");
        return sb.toString();
    }


    private void writeObject(java.io.ObjectOutputStream oos)
        throws IOException
    {
        oos.defaultWriteObject(); // location

        // Serialize the array of certs
        if (certs == null || certs.length == 0) {
            oos.writeInt(0);
        } else {
            // write out the total number of certs
            oos.writeInt(certs.length);
            // write out each cert, including its type
            for (int i = 0; i < certs.length; i++) {
                java.security.cert.Certificate cert = certs[i];
                try {
                    oos.writeUTF(cert.getType());
                    byte[] encoded = cert.getEncoded();
                    oos.writeInt(encoded.length);
                    oos.write(encoded);
                } catch (CertificateEncodingException cee) {
                    throw new IOException(cee.getMessage());
                }
            }
        }

        // Serialize the array of code signers (if any)
        if (signers != null && signers.length > 0) {
            oos.writeObject(signers);
        }
    }


    private void readObject(java.io.ObjectInputStream ois)
        throws IOException, ClassNotFoundException
    {
        CertificateFactory cf;
        Hashtable<String, CertificateFactory> cfs = null;
        List<java.security.cert.Certificate> certList = null;

        ois.defaultReadObject(); // location

        // process any new-style certs in the stream (if present)
        int size = ois.readInt();
        if (size > 0) {
            // we know of 3 different cert types: X.509, PGP, SDSI, which
            // could all be present in the stream at the same time
            cfs = new Hashtable<>(3);
            certList = new ArrayList<>(size > 20 ? 20 : size);
        }

        for (int i = 0; i < size; i++) {
            // read the certificate type, and instantiate a certificate
            // factory of that type (reuse existing factory if possible)
            String certType = ois.readUTF();
            if (cfs.containsKey(certType)) {
                // reuse certificate factory
                cf = cfs.get(certType);
            } else {
                // create new certificate factory
                try {
                    cf = CertificateFactory.getInstance(certType);
                } catch (CertificateException ce) {
                    throw new ClassNotFoundException
                        ("Certificate factory for " + certType + " not found");
                }
                // store the certificate factory so we can reuse it later
                cfs.put(certType, cf);
            }
            // parse the certificate
            byte[] encoded = null;
            try {
                encoded = new byte[ois.readInt()];
            } catch (OutOfMemoryError oome) {
                throw new IOException("Certificate too big");
            }
            ois.readFully(encoded);
            ByteArrayInputStream bais = new ByteArrayInputStream(encoded);
            try {
                certList.add(cf.generateCertificate(bais));
            } catch (CertificateException ce) {
                throw new IOException(ce.getMessage());
            }
            bais.close();
        }

        if (certList != null) {
            this.certs = certList.toArray(
                    new java.security.cert.Certificate[size]);
        }
        // Deserialize array of code signers (if any)
        try {
            this.signers = ((CodeSigner[])ois.readObject()).clone();
        } catch (IOException ioe) {
            // no signers present
        }

        if (location != null) {
            locationNoFragString = URLUtil.urlNoFragString(location);
        }
    }

    /*
     * Convert an array of certificates to an array of code signers.
     * The array of certificates is a concatenation of certificate chains
     * where the initial certificate in each chain is the end-entity cert.
     *
     * @return an array of code signers or null if none are generated.
     */
    private CodeSigner[] convertCertArrayToSignerArray(
        java.security.cert.Certificate[] certs) {

        if (certs == null) {
            return null;
        }

        try {
            // Initialize certificate factory
            if (factory == null) {
                factory = CertificateFactory.getInstance("X.509");
            }

            // Iterate through all the certificates
            int i = 0;
            List<CodeSigner> signers = new ArrayList<>();
            while (i < certs.length) {
                List<java.security.cert.Certificate> certChain =
                        new ArrayList<>();
                certChain.add(certs[i++]); // first cert is an end-entity cert
                int j = i;

                // Extract chain of certificates
                // (loop while certs are not end-entity certs)
                while (j < certs.length &&
                    certs[j] instanceof X509Certificate &&
                    ((X509Certificate)certs[j]).getBasicConstraints() != -1) {
                    certChain.add(certs[j]);
                    j++;
                }
                i = j;
                CertPath certPath = factory.generateCertPath(certChain);
                signers.add(new CodeSigner(certPath, null));
            }

            if (signers.isEmpty()) {
                return null;
            } else {
                return signers.toArray(new CodeSigner[signers.size()]);
            }

        } catch (CertificateException e) {
            return null; //TODO - may be better to throw an ex. here
        }
    }
}
