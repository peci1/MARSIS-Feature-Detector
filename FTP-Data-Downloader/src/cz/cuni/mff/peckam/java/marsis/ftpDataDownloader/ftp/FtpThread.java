/**
 * 
 */
package cz.cuni.mff.peckam.java.marsis.ftpDataDownloader.ftp;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cz.dhl.ftp.Ftp;
import cz.dhl.ftp.FtpConnect;

/**
 * A thread for FTP communication.
 * 
 * At the beginning of the run() method, the FtpClient is ready, but not
 * connected.
 * 
 * @author Martin Pecka
 */
public abstract class FtpThread extends Thread
{
    /**
     * The FTP client used for connection
     */
    protected Ftp ftpClient = null;

    /**
     * @return The FTP client used for communication
     */
    public Ftp getFtpClient()
    {
        return ftpClient;
    }

    /**
     * FTP connection settings
     */
    protected FtpConnect connect             = null;

    /**
     * Listeners to the change of percentage
     */
    List<ChangeListener> percentageListeners = new LinkedList<ChangeListener>();

    /**
     * The percentage done
     */
    protected double     percentage          = 0;

    /**
     * @return The connection settings
     */
    public FtpConnect getConnect()
    {
        return connect;
    }

    /**
     * @param ftpClient the ftpClient to set
     */
    public void setFtpClient(Ftp ftpClient)
    {
        this.ftpClient = ftpClient;
    }

    /**
     * @param connect the connect to set
     */
    public void setConnect(FtpConnect connect)
    {
        this.connect = connect;
    }

    /**
     * Constructs a new FTP thread and prepares the FTP client
     */
    public FtpThread()
    {
        connect = new FtpConnect();
        connect.setHostName("psa.esac.esa.int");
        connect.setPortNum(21);
        connect.setUserName("anonymous");
        connect.setPassWord("anonymous");
        ftpClient = new Ftp();
        ftpClient.getContext().setActiveSocketMode(false);
        ftpClient.getContext().setConsole(null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable
    {
        if (ftpClient.isConnected())
            ftpClient.disconnect();
        super.finalize();
    }

    /**
     * @param percentage The percenatge to set (0.0 - 100.0)
     */
    public void setPercentage(double percentage)
    {
        this.percentage = percentage;
        onPercentageChanged();
    }

    /**
     * Called when the percentage is changed
     */
    protected void onPercentageChanged()
    {
        for (ChangeListener l : percentageListeners)
            l.stateChanged(new ChangeEvent(this));
    }

}
