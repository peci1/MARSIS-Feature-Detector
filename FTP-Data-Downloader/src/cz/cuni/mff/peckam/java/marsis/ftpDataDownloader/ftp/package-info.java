/**
 * This package provides support for FTP operations.
 * 
 * The most important is FtpThread, which is a thread prepared for FTP
 * communication. If you want to do some FTP operations, just subclass FtpThread
 * and write the commands. FtpThread DOES NOT open the connection by default, it
 * is on your own.
 * 
 * The other classes are mainly support for upload using Executor and for
 * reusing open FTP connections to do more work.
 */
package cz.cuni.mff.peckam.java.marsis.ftpDataDownloader.ftp;