package org.kendar.apis.utils;

import java.io.*;
import java.nio.charset.Charset;


/**
 * Simple utility methods for dealing with streams. The copy methods of this class are
 * similar to those defined in  except that all affected streams are
 * left open when done. All copy methods use a block size of 8192 bytes.
 *
 * <p>Mainly for use within the framework, but also useful for application code.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 3.2.2
 */
public abstract class StreamUtils {

    /**
     * The default buffer size used when copying bytes.
     */
    public static final int BUFFER_SIZE = 8192;

    private static final byte[] EMPTY_CONTENT = new byte[0];


    /**
     * Copy the contents of the given InputStream into a new byte array.
     * <p>Leaves the stream open when done.
     *
     * @param in the stream to copy from (may be {@code null} or empty)
     * @return the new byte array that has been copied to (possibly empty)
     * @throws IOException in case of I/O errors
     */
    public static byte[] copyToByteArray(InputStream in) throws IOException {
        if (in == null) {
            return EMPTY_CONTENT;
        }

        return in.readAllBytes();
    }

    /**
     * Copy the contents of the given InputStream into a String.
     * <p>Leaves the stream open when done.
     *
     * @param in      the InputStream to copy from (may be {@code null} or empty)
     * @param charset the {@link Charset} to use to decode the bytes
     * @return the String that has been copied to (possibly empty)
     * @throws IOException in case of I/O errors
     */
    public static String copyToString(InputStream in, Charset charset) throws IOException {
        if (in == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(in, charset);
        char[] buffer = new char[BUFFER_SIZE];
        int charsRead;
        while ((charsRead = reader.read(buffer)) != -1) {
            out.append(buffer, 0, charsRead);
        }
        return out.toString();
    }

    /**
     * Copy the contents of the given {@link ByteArrayOutputStream} into a {@link String}.
     * <p>This is a more effective equivalent of {@code new String(baos.toByteArray(), charset)}.
     *
     * @param baos    the {@code ByteArrayOutputStream} to be copied into a String
     * @param charset the {@link Charset} to use to decode the bytes
     * @return the String that has been copied to (possibly empty)
     * @since 5.2.6
     */
    public static String copyToString(ByteArrayOutputStream baos, Charset charset) {

        return baos.toString(charset);
    }

    /**
     * Copy the contents of the given byte array to the given OutputStream.
     * <p>Leaves the stream open when done.
     *
     * @param in  the byte array to copy from
     * @param out the OutputStream to copy to
     * @throws IOException in case of I/O errors
     */
    public static void copy(byte[] in, OutputStream out) throws IOException {

        out.write(in);
        out.flush();
    }

    /**
     * Copy the contents of the given String to the given OutputStream.
     * <p>Leaves the stream open when done.
     *
     * @param in      the String to copy from
     * @param charset the Charset
     * @param out     the OutputStream to copy to
     * @throws IOException in case of I/O errors
     */
    public static void copy(String in, Charset charset, OutputStream out) throws IOException {

        out.write(in.getBytes(charset));
        out.flush();
    }

    /**
     * Copy the contents of the given InputStream to the given OutputStream.
     * <p>Leaves both streams open when done.
     *
     * @param in  the InputStream to copy from
     * @param out the OutputStream to copy to
     * @return the number of bytes copied
     * @throws IOException in case of I/O errors
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {

        int count = (int) in.transferTo(out);
        out.flush();
        return count;
    }

    /**
     * Copy a range of content of the given InputStream to the given OutputStream.
     * <p>If the specified range exceeds the length of the InputStream, this copies
     * up to the end of the stream and returns the actual number of copied bytes.
     * <p>Leaves both streams open when done.
     *
     * @param in    the InputStream to copy from
     * @param out   the OutputStream to copy to
     * @param start the position to start copying from
     * @param end   the position to end copying
     * @return the number of bytes copied
     * @throws IOException in case of I/O errors
     * @since 4.3
     */
    public static long copyRange(InputStream in, OutputStream out, long start, long end) throws IOException {

        long skipped = in.skip(start);
        if (skipped < start) {
            throw new IOException("Skipped only " + skipped + " bytes out of " + start + " required");
        }

        long bytesToCopy = end - start + 1;
        byte[] buffer = new byte[(int) Math.min(StreamUtils.BUFFER_SIZE, bytesToCopy)];
        while (bytesToCopy > 0) {
            int bytesRead = (bytesToCopy < buffer.length ? in.read(buffer, 0, (int) bytesToCopy) :
                    in.read(buffer));
            if (bytesRead == -1) {
                break;
            }
            out.write(buffer, 0, bytesRead);
            bytesToCopy -= bytesRead;
        }
        return (end - start + 1 - bytesToCopy);
    }

    /**
     * Drain the remaining content of the given InputStream.
     * <p>Leaves the InputStream open when done.
     *
     * @param in the InputStream to drain
     * @return the number of bytes read
     * @throws IOException in case of I/O errors
     * @since 4.3
     */
    //@Contract("null -> fail")
    public static int drain(InputStream in) throws IOException {
        if (in == null) {
            return 0;
        }
        return (int) in.transferTo(OutputStream.nullOutputStream());
    }

    /**
     * Return an efficient empty {@link InputStream}.
     *
     * @return an InputStream which contains no bytes
     * @since 4.2.2
     * @deprecated as of 6.0 in favor of {@link InputStream#nullInputStream()}
     */
    @Deprecated(since = "6.0")
    public static InputStream emptyInput() {
        return InputStream.nullInputStream();
    }

    /**
     * Return a variant of the given {@link InputStream} where calling
     * {@link InputStream#close() close()} has no effect.
     *
     * @param in the InputStream to decorate
     * @return a version of the InputStream that ignores calls to close
     */
    public static InputStream nonClosing(InputStream in) {
        //Assert.notNull(in, "No InputStream specified");
        return new NonClosingInputStream(in);
    }

    /**
     * Return a variant of the given {@link OutputStream} where calling
     * {@link OutputStream#close() close()} has no effect.
     *
     * @param out the OutputStream to decorate
     * @return a version of the OutputStream that ignores calls to close
     */
    public static OutputStream nonClosing(OutputStream out) {
        //Assert.notNull(out, "No OutputStream specified");
        return new NonClosingOutputStream(out);
    }


    private static class NonClosingInputStream extends FilterInputStream {

        public NonClosingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
        }
    }


    private static class NonClosingOutputStream extends FilterOutputStream {

        public NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int let) throws IOException {
            // It is critical that we override this method for performance
            this.out.write(b, off, let);
        }

        @Override
        public void close() throws IOException {
        }
    }

}