/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import sun.security.action.GetPropertyAction;



public final class ProcessBuilder
{
    private List<String> command;
    private File directory;
    private Map<String,String> environment;
    private boolean redirectErrorStream;
    private Redirect[] redirects;


    public ProcessBuilder(List<String> command) {
        if (command == null)
            throw new NullPointerException();
        this.command = command;
    }


    public ProcessBuilder(String... command) {
        this.command = new ArrayList<>(command.length);
        for (String arg : command)
            this.command.add(arg);
    }


    public ProcessBuilder command(List<String> command) {
        if (command == null)
            throw new NullPointerException();
        this.command = command;
        return this;
    }


    public ProcessBuilder command(String... command) {
        this.command = new ArrayList<>(command.length);
        for (String arg : command)
            this.command.add(arg);
        return this;
    }


    public List<String> command() {
        return command;
    }


    public Map<String,String> environment() {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkPermission(new RuntimePermission("getenv.*"));

        if (environment == null)
            environment = ProcessEnvironment.environment();

        assert environment != null;

        return environment;
    }

    // Only for use by Runtime.exec(...envp...)
    ProcessBuilder environment(String[] envp) {
        assert environment == null;
        if (envp != null) {
            environment = ProcessEnvironment.emptyEnvironment(envp.length);
            assert environment != null;

            for (String envstring : envp) {
                // Before 1.5, we blindly passed invalid envstrings
                // to the child process.
                // We would like to throw an exception, but do not,
                // for compatibility with old broken code.

                // Silently discard any trailing junk.
                if (envstring.indexOf((int) '\u0000') != -1)
                    envstring = envstring.replaceFirst("\u0000.*", "");

                int eqlsign =
                    envstring.indexOf('=', ProcessEnvironment.MIN_NAME_LENGTH);
                // Silently ignore envstrings lacking the required `='.
                if (eqlsign != -1)
                    environment.put(envstring.substring(0,eqlsign),
                                    envstring.substring(eqlsign+1));
            }
        }
        return this;
    }


    public File directory() {
        return directory;
    }


    public ProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }

    // ---------------- I/O Redirection ----------------


    static class NullInputStream extends InputStream {
        static final NullInputStream INSTANCE = new NullInputStream();
        private NullInputStream() {}
        public int read()      { return -1; }
        public int available() { return 0; }
    }


    static class NullOutputStream extends OutputStream {
        static final NullOutputStream INSTANCE = new NullOutputStream();
        private NullOutputStream() {}
        public void write(int b) throws IOException {
            throw new IOException("Stream closed");
        }
    }


    public abstract static class Redirect {
        private static final File NULL_FILE = new File(
                (GetPropertyAction.privilegedGetProperty("os.name")
                        .startsWith("Windows") ? "NUL" : "/dev/null")
        );


        public enum Type {

            PIPE,


            INHERIT,


            READ,


            WRITE,


            APPEND
        };


        public abstract Type type();


        public static final Redirect PIPE = new Redirect() {
                public Type type() { return Type.PIPE; }
                public String toString() { return type().toString(); }};


        public static final Redirect INHERIT = new Redirect() {
                public Type type() { return Type.INHERIT; }
                public String toString() { return type().toString(); }};



        public static final Redirect DISCARD = new Redirect() {
                public Type type() { return Type.WRITE; }
                public String toString() { return type().toString(); }
                public File file() { return NULL_FILE; }
                boolean append() { return false; }
        };


        public File file() { return null; }


        boolean append() {
            throw new UnsupportedOperationException();
        }


        public static Redirect from(final File file) {
            if (file == null)
                throw new NullPointerException();
            return new Redirect() {
                    public Type type() { return Type.READ; }
                    public File file() { return file; }
                    public String toString() {
                        return "redirect to read from file \"" + file + "\"";
                    }
                };
        }


        public static Redirect to(final File file) {
            if (file == null)
                throw new NullPointerException();
            return new Redirect() {
                    public Type type() { return Type.WRITE; }
                    public File file() { return file; }
                    public String toString() {
                        return "redirect to write to file \"" + file + "\"";
                    }
                    boolean append() { return false; }
                };
        }


        public static Redirect appendTo(final File file) {
            if (file == null)
                throw new NullPointerException();
            return new Redirect() {
                    public Type type() { return Type.APPEND; }
                    public File file() { return file; }
                    public String toString() {
                        return "redirect to append to file \"" + file + "\"";
                    }
                    boolean append() { return true; }
                };
        }


        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (! (obj instanceof Redirect))
                return false;
            Redirect r = (Redirect) obj;
            if (r.type() != this.type())
                return false;
            assert this.file() != null;
            return this.file().equals(r.file());
        }


        public int hashCode() {
            File file = file();
            if (file == null)
                return super.hashCode();
            else
                return file.hashCode();
        }


        private Redirect() {}
    }


    static class RedirectPipeImpl extends Redirect {
        final FileDescriptor fd;

        RedirectPipeImpl() {
            this.fd = new FileDescriptor();
        }
        @Override
        public Type type() { return Type.PIPE; }

        @Override
        public String toString() { return type().toString();}

        FileDescriptor getFd() { return fd; }
    }


    private Redirect[] redirects() {
        if (redirects == null) {
            redirects = new Redirect[] {
                    Redirect.PIPE, Redirect.PIPE, Redirect.PIPE
            };
        }
        return redirects;
    }


    public ProcessBuilder redirectInput(Redirect source) {
        if (source.type() == Redirect.Type.WRITE ||
            source.type() == Redirect.Type.APPEND)
            throw new IllegalArgumentException(
                "Redirect invalid for reading: " + source);
        redirects()[0] = source;
        return this;
    }


    public ProcessBuilder redirectOutput(Redirect destination) {
        if (destination.type() == Redirect.Type.READ)
            throw new IllegalArgumentException(
                "Redirect invalid for writing: " + destination);
        redirects()[1] = destination;
        return this;
    }


    public ProcessBuilder redirectError(Redirect destination) {
        if (destination.type() == Redirect.Type.READ)
            throw new IllegalArgumentException(
                "Redirect invalid for writing: " + destination);
        redirects()[2] = destination;
        return this;
    }


    public ProcessBuilder redirectInput(File file) {
        return redirectInput(Redirect.from(file));
    }


    public ProcessBuilder redirectOutput(File file) {
        return redirectOutput(Redirect.to(file));
    }


    public ProcessBuilder redirectError(File file) {
        return redirectError(Redirect.to(file));
    }


    public Redirect redirectInput() {
        return (redirects == null) ? Redirect.PIPE : redirects[0];
    }


    public Redirect redirectOutput() {
        return (redirects == null) ? Redirect.PIPE : redirects[1];
    }


    public Redirect redirectError() {
        return (redirects == null) ? Redirect.PIPE : redirects[2];
    }


    public ProcessBuilder inheritIO() {
        Arrays.fill(redirects(), Redirect.INHERIT);
        return this;
    }


    public boolean redirectErrorStream() {
        return redirectErrorStream;
    }


    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }


    public Process start() throws IOException {
        return start(redirects);
    }


    private Process start(Redirect[] redirects) throws IOException {
        // Must convert to array first -- a malicious user-supplied
        // list might try to circumvent the security check.
        String[] cmdarray = command.toArray(new String[command.size()]);
        cmdarray = cmdarray.clone();

        for (String arg : cmdarray)
            if (arg == null)
                throw new NullPointerException();
        // Throws IndexOutOfBoundsException if command is empty
        String prog = cmdarray[0];

        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkExec(prog);

        String dir = directory == null ? null : directory.toString();

        for (int i = 1; i < cmdarray.length; i++) {
            if (cmdarray[i].indexOf('\u0000') >= 0) {
                throw new IOException("invalid null character in command");
            }
        }

        try {
            return ProcessImpl.start(cmdarray,
                                     environment,
                                     dir,
                                     redirects,
                                     redirectErrorStream);
        } catch (IOException | IllegalArgumentException e) {
            String exceptionInfo = ": " + e.getMessage();
            Throwable cause = e;
            if ((e instanceof IOException) && security != null) {
                // Can not disclose the fail reason for read-protected files.
                try {
                    security.checkRead(prog);
                } catch (SecurityException se) {
                    exceptionInfo = "";
                    cause = se;
                }
            }
            // It's much easier for us to create a high-quality error
            // message than the low-level C code which found the problem.
            throw new IOException(
                "Cannot run program \"" + prog + "\""
                + (dir == null ? "" : " (in directory \"" + dir + "\")")
                + exceptionInfo,
                cause);
        }
    }


    public static List<Process> startPipeline(List<ProcessBuilder> builders) throws IOException {
        // Accumulate and check the builders
        final int numBuilders = builders.size();
        List<Process> processes = new ArrayList<>(numBuilders);
        try {
            Redirect prevOutput = null;
            for (int index = 0; index < builders.size(); index++) {
                ProcessBuilder builder = builders.get(index);
                Redirect[] redirects = builder.redirects();
                if (index > 0) {
                    // check the current Builder to see if it can take input from the previous
                    if (builder.redirectInput() != Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectInput()" +
                                " must be PIPE except for the first builder: "
                                + builder.redirectInput());
                    }
                    redirects[0] = prevOutput;
                }
                if (index < numBuilders - 1) {
                    // check all but the last stage has output = PIPE
                    if (builder.redirectOutput() != Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectOutput()" +
                                " must be PIPE except for the last builder: "
                                + builder.redirectOutput());
                    }
                    redirects[1] = new RedirectPipeImpl();  // placeholder for new output
                }
                processes.add(builder.start(redirects));
                prevOutput = redirects[1];
            }
        } catch (Exception ex) {
            // Cleanup processes already started
            processes.forEach(Process::destroyForcibly);
            processes.forEach(p -> {
                try {
                    p.waitFor();        // Wait for it to exit
                } catch (InterruptedException ie) {
                    // If interrupted; continue with next Process
                    Thread.currentThread().interrupt();
                }
            });
            throw ex;
        }
        return processes;
    }
}
