/**
 * 
 */
package cz.cuni.mff.peckam.java.marsis.ftpDataDownloader.ftp;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This executor handles FTP uploads. Each worker thread will hold its own FTP
 * connection and will provide it to its tasks.
 * 
 * @author Martin Pecka
 */
public class FtpExecutor extends ThreadPoolExecutor
{

    /**
     * Create a new FTP upload executor with nThreads threads
     * 
     * @param nThreads The number of threads to use
     */
    public FtpExecutor(int nThreads)
    {
        super(nThreads, nThreads, 30000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r)
                    {
                        final Runnable rr = r;
                        try {
                            Thread t = new FtpExecutorThread() {
                                @Override
                                public void run()
                                {
                                    rr.run();
                                }
                            };
                            return t;
                        } catch (IOException e) {
                            return null;
                        }
                    }
                });
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r)
    {
        super.beforeExecute(t, r);
        if (r instanceof FtpThread && t instanceof FtpExecutorThread) {
            FtpThread f = (FtpThread) r;
            FtpExecutorThread e = (FtpExecutorThread) t;
            f.setConnect(e.getConnect());
            f.setFtpClient(e.getFtpClient());
        }
    }

}
