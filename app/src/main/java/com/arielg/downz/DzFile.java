package com.arielg.downz;

import java.net.URL;

class DzFile {

    private static final int MAX_PRESENTABLE_URL_LEN = 52;

    private URL             mUrl = null;
    private String          mFileName = "";
    private String          mPresentableUrl = "";
    private String          mPresentableFileName = "";
    private DzFileStatus    mStatus = DzFileStatus.INVALID;
    private Integer         mDownloadedPercentage = 0;

    public DzFile(URL url) {
        super();
        this.mUrl = url;
        this.mFileName = url.toString().substring(url.toString().lastIndexOf('/')+1);
        this.mPresentableUrl = createPresentableUrl(url);
        this.mPresentableFileName = createPresentableLine(this.mFileName);
        this.mStatus = DzFileStatus.READY;
    }

    public URL getUrl() {
        return mUrl;
    }

    public String getFileName() { return this.mFileName; }

    public String getPresentableUrl() {
        switch (this.mStatus) {
            case INVALID:       return "[I] " + mPresentableUrl;
            case READY:         return "[R] " + mPresentableUrl;
            case DOWNLOADING:   return "[D] " + mPresentableUrl;
            case CANCELLED:     return "[C] " + mPresentableUrl;
            case FINISHED:      return "[F] " + mPresentableUrl;
            case ERROR:         return "[E] " + mPresentableUrl;
            default:            return "[?] " + mPresentableUrl;
        }
    }

    public String getPresentableFileName() {
        switch (this.mStatus) {
            case INVALID:       return "[I] " + mPresentableFileName;
            case READY:         return "[R] " + mPresentableFileName;
            case DOWNLOADING:   return "[D] " + mPresentableFileName;
            case CANCELLED:     return "[C] " + mPresentableFileName;
            case FINISHED:      return "[F] " + mPresentableFileName;
            case ERROR:         return "[E] " + mPresentableFileName;
            default:            return "[?] " + mPresentableFileName;
        }
    }

    public DzFileStatus getStatus() {
        return mStatus;
    }
    public void setStatus(DzFileStatus status) {
        this.mStatus = status;
    }

    public Integer getDownloadedPercentage() { return mDownloadedPercentage; }
    public void setDownloadedPercentage(Integer downloadedPercentage) { mDownloadedPercentage = downloadedPercentage; }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    public void resetStatus() {
        this.mStatus = DzFileStatus.READY;
        this.mDownloadedPercentage = 0;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private String createPresentableUrl(URL url) {
        return createPresentableLine(url.toString());
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private String createPresentableLine(String sUrl) {
        if(sUrl.length() > MAX_PRESENTABLE_URL_LEN+3) {
            return sUrl.substring(0, (MAX_PRESENTABLE_URL_LEN/2)) + "..." + sUrl.substring(sUrl.length()-(MAX_PRESENTABLE_URL_LEN/2));
        }
        return sUrl;
    }
}
