/**
 * 
 */
package cz.cuni.mff.peckam.java.marsis.ftpDataDownloader.ftp;

import java.io.File;
import java.io.IOException;

import cz.cuni.mff.peckam.java.marsis.ftpDataDownloader.FTPDataDownloader;
import cz.dhl.ftp.FtpFile;
import cz.dhl.io.CoLoad;
import cz.dhl.io.LocalFile;

/**
 * A thread that downloads the given remoteFile to the given local file.
 * 
 * @author Martin Pecka
 */
public class FtpDownloadThread extends FtpThread
{
    /** The number of retries if an upload fails. */
    protected static final int NUM_RETRIES          = 10;

    /** The delay between two retries (in miliseconds). */
    protected static final int RETRY_DELAY          = 100;
    /** The number of ms to add to RETRY_DELAY every time an upload fails. */
    protected static final int RETRY_ADDITIVE_DELAY = 100;

    /**
     * The remoteFile to download
     */
    protected String           remoteFile           = null;

    /**
     * The directory to download from.
     */
    protected String           dir                  = "";

    /** The file to download to. */
    protected String           localFile            = null;

    /**
     * @param remoteFile The remoteFile to upload
     * @param dir The directory to download from.
     * @param localFile The file to download to.
     */
    public FtpDownloadThread(String remoteFile, String dir, String localFile)
    {
        this.remoteFile = remoteFile;
        this.dir = dir;
        this.localFile = localFile;
    }

    /**
     * Called when the file has been downloaded. Designed to be overridden.
     * 
     * @param file The file that was downloaded.
     * @param localFile Length of the file in bytes.
     */
    public void onFileDownloaded(FtpFile file, File localFile)
    {
        FTPDataDownloader.anotherFileDownloaded(localFile.length());
    }

    @Override
    public void run()
    {
        // downloadExecutorExecutor provides open FTP connection before running the task
        // the connection can be get by this.getFtpClient()
        try {
            getFtpClient().connect(getConnect());
            String path = dir;
            if (!getFtpClient().cd(path)) {
                throw new IOException("Cannot cd to directory " + path);
            }

            FtpFile ftpFile = new FtpFile(path + "/" + remoteFile, getFtpClient());
            LocalFile localFile = new LocalFile(this.localFile);

            // try to upload the remoteFile
            int delay = RETRY_DELAY;
            boolean downloaded = false;
            for (int i = 0; i < NUM_RETRIES; i++) {
                if (CoLoad.copy(localFile, ftpFile)) {
                    downloaded = true;
                    break;
                }

                try {
                    synchronized (this) {
                        wait(delay);
                    }
                } catch (InterruptedException e) {}
                delay += RETRY_ADDITIVE_DELAY;
            }

            // if the download hasn't been successful, throw an exception
            if (!downloaded)
                throw new IOException("Download failed");

            onFileDownloaded(ftpFile, new File(this.localFile));

        } catch (IOException e) {
            System.err.println(e.getMessage());
            this.setPercentage(100);
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        // disable autodisconnect on thread finalizing
    }
}
