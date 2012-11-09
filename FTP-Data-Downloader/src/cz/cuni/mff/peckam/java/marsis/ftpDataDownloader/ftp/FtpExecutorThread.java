/**
 * 
 */
package cz.cuni.mff.peckam.java.marsis.ftpDataDownloader.ftp;

import java.io.IOException;

/**
 * A thread that will execute FTP upload tasks using FtpExecutor.
 * 
 * @author Martin Pecka
 */
public class FtpExecutorThread extends FtpThread
{
    /**
     * Connect to the FTP server
     * 
     * @throws IOException If the connection failed
     */
    public FtpExecutorThread() throws IOException
    {
        getFtpClient().connect(getConnect());
    }
}
