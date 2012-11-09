/**
 * 
 */
package cz.cuni.mff.peckam.java.marsis.ftpDataDownloader;

import java.io.File;

import cz.cuni.mff.peckam.java.marsis.ftpDataDownloader.ftp.FtpDownloadThread;
import cz.cuni.mff.peckam.java.marsis.ftpDataDownloader.ftp.FtpExecutor;
import cz.cuni.mff.peckam.java.marsis.ftpDataDownloader.ftp.FtpThread;
import cz.dhl.ftp.FtpFile;
import cz.dhl.io.CoFile;

/**
 * 
 * 
 * @author Martin Pecka
 */
public class FTPDataDownloader
{

    private static final String  IMAGE_DIR       = "/pub/mirror/MARS-EXPRESS/MARSIS/MEX-M-MARSIS-3-RDR-AIS-V1.0/BROWSE/ACTIVE_IONOSPHERIC_SOUNDER/";
    private static final String  DATA_DIR        = "/pub/mirror/MARS-EXPRESS/MARSIS/MEX-M-MARSIS-3-RDR-AIS-V1.0/DATA/ACTIVE_IONOSPHERIC_SOUNDER/";

    private static volatile int  downloadedFiles = 0;
    private static volatile long downloadedBytes = 0;

    /**
     * 
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        final FtpExecutor executorService = new FtpExecutor(10);
        final Integer start = Integer.parseInt(args[0]);
        final Integer end = Integer.parseInt(args[1]);

        new File("data").mkdir();

        executorService.execute(new FtpThread() {

            @Override
            public void run()
            {
                for (int i = start; i <= end; i++) {
                    String iString = ((Integer) i).toString();
                    String tenFolderName = iString.substring(0, iString.length() - 1) + "X";

                    if (i == start || i % 10 == 0) {
                        FtpFile dataDirContents = new FtpFile(DATA_DIR + "/RDR" + tenFolderName, getFtpClient());
                        CoFile[] dataDirFiles = dataDirContents.listCoFiles();
                        if (dataDirFiles != null) {
                            new File("data\\" + tenFolderName).mkdir();
                            for (CoFile file : dataDirFiles) {
                                executorService.execute(new FtpDownloadThread(file.getName(), dataDirContents
                                        .getAbsolutePath(), "data/" + tenFolderName + "/" + file.getName()));
                            }
                        }
                    }

                    FtpFile imageDirContents = new FtpFile(IMAGE_DIR + "/RDR" + tenFolderName + "/RDR" + iString,
                            getFtpClient());
                    CoFile[] imageDirFiles = imageDirContents.listCoFiles();
                    if (imageDirFiles != null) {
                        new File("data\\" + tenFolderName + "\\" + iString).mkdir();
                        for (CoFile file : imageDirFiles) {
                            executorService.execute(new FtpDownloadThread(file.getName(), imageDirContents
                                    .getAbsolutePath(), "data/" + tenFolderName + "/" + iString + "/" + file.getName()));
                        }
                    }
                }
            }
        });
    }

    public static synchronized void anotherFileDownloaded(long size)
    {
        downloadedBytes += size;
        System.err.println("Downloaded " + (++downloadedFiles) + " files, " + downloadedBytes + " bytes");
    }
}