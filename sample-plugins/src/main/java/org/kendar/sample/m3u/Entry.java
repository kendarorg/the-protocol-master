package org.kendar.sample.m3u;

/**
 * Created by tgermain on 30/12/2017.
 */
public class Entry {

    private String duration;
    private String groupTitle;
    private String tvgId;
    private String tvgName;
    private String tvgLogo;
    private String tvgShift;
    private String radio;
    private String channelUri;
    private String channelName;

    private Entry() {
    }


    /**
     * duration of the
     *
     * @return the duration or null
     */
    public String getDuration() {
        return duration;
    }

    /**
     * group-title is channels group name
     *
     * @return the group-title or null if it doesn't exist
     */
    public String getGroupTitle() {
        return groupTitle;
    }

    /**
     * tvg-id is value of channel id in EPG xml file
     *
     * @return the tvg-id value or null if it doesn't exist
     */
    public String getTvgId() {
        return tvgId;
    }

    /**
     * tvg-name is value of display-name in EPG
     *
     * @return the tvg-name value or null if it doesn't exist
     */
    public String getTvgName() {
        return tvgName;
    }

    /**
     * tvg-logo is the path to the channel logo file
     *
     * @return the tvg-logo value or null if it doesn't exist
     */
    public String getTvgLogo() {
        return tvgLogo;
    }

    /**
     * tvg-shift is value in hours to shift EPG time.
     *
     * @return the tvg-shift value or null if it doesn't exist
     */
    public String getTvgShift() {
        return tvgShift;
    }

    /**
     * radio indicates if the channel is a radio or not
     *
     * @return the radio value or null if it doesn't exist
     */
    public String getRadio() {
        return radio;
    }

    /**
     * the uri can be a relative or absolute path. It can also be an URL
     *
     * @return the channel URI
     */
    public String getChannelUri() {
        return channelUri;
    }

    /**
     * channel name of the channel
     *
     * @return the channel name
     */
    public String getChannelName() {
        return channelName;
    }

    @Override
    public String toString() {
        return "Entry{" +
                "duration='" + duration + '\'' +
                ", groupTitle='" + groupTitle + '\'' +
                ", tvgId='" + tvgId + '\'' +
                ", tvgName='" + tvgName + '\'' +
                ", tvgLogo='" + tvgLogo + '\'' +
                ", tvgShift='" + tvgShift + '\'' +
                ", radio='" + radio + '\'' +
                ", channelUri='" + channelUri + '\'' +
                ", channelName='" + channelName + '\'' +
                '}';
    }

    public static class Builder {
        private String duration;
        private String name;
        private String groupTitle;
        private String tvgId;
        private String tvgName;
        private String tvgLogo;
        private String tvgShift;
        private String radio;
        private String channelUri;
        private String channelName;

        public Builder duration(String duration) {
            this.duration = duration;
            return this;
        }

        public Builder groupTitle(String groupTitle) {
            this.groupTitle = groupTitle;
            return this;
        }

        public Builder tvgId(String tvgId) {
            this.tvgId = tvgId;
            return this;
        }

        public Builder tvgName(String tvgName) {
            this.tvgName = tvgName;
            return this;
        }

        public Builder tvgLogo(String tvgLogo) {
            this.tvgLogo = tvgLogo;
            return this;
        }

        public Builder tvgShift(String tvgShift) {
            this.tvgShift = tvgShift;
            return this;
        }

        public Builder radio(String radio) {
            this.radio = radio;
            return this;
        }

        public Builder channelUri(String channelUri) {
            this.channelUri = channelUri;
            return this;
        }

        public Builder channelName(String channelName) {
            this.channelName = channelName;
            return this;
        }

        public Entry build() {
            Entry entry = new Entry();
            entry.duration = duration;
            entry.groupTitle = groupTitle;
            entry.tvgId = tvgId;
            entry.tvgName = tvgName;
            entry.tvgLogo = tvgLogo;
            entry.tvgShift = tvgShift;
            entry.radio = radio;
            entry.channelUri = channelUri;
            entry.channelName = channelName;
            return entry;
        }
    }
}
