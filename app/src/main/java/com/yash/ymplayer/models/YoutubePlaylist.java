package com.yash.ymplayer.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class YoutubePlaylist implements Serializable {
    private String nextPageToken;
    private String prevPageToken;
    ArrayList<PlayListItem> items = new ArrayList<>();

    public String getPrevPageToken() {
        return prevPageToken;
    }

    public void setPrevPageToken(String prevPageToken) {
        this.prevPageToken = prevPageToken;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public ArrayList<PlayListItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<PlayListItem> items) {
        this.items = items;
    }

    public class PlayListItem {
        Snippet snippet;

        public Snippet getSnippet() {
            return snippet;
        }

        public void setSnippet(Snippet snippet) {
            this.snippet = snippet;
        }

        public class Snippet {
            private String title;
            private String channelTitle;
            private ResourceId resourceId;
            private Thumbnails thumbnails;

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public Thumbnails getThumbnails() {
                return thumbnails;
            }

            public void setThumbnails(Thumbnails thumbnails) {
                this.thumbnails = thumbnails;
            }

            public String getChannelTitle() {
                return channelTitle;
            }

            public void setChannelTitle(String channelTitle) {
                this.channelTitle = channelTitle;
            }

            public ResourceId getResourceId() {
                return resourceId;
            }

            public void setResourceId(ResourceId resourceIdObject) {
                resourceId = resourceIdObject;
            }

            public class ResourceId {
                private String videoId;

                public String getVideoId() {
                    return videoId;
                }
                public void setVideoId(String videoId) {
                    this.videoId = videoId;
                }
            }

            public class Thumbnails {
                @SerializedName("default")
                Default def;
                Medium medium;
                High high;

                // Getter Methods

                public Default getDefault() {
                    return def;
                }

                public Medium getMedium() {
                    return medium;
                }

                public High getHigh() {
                    return high;
                }

                // Setter Methods

                public void setDefault(Default def) {
                    this.def = def;
                }

                public void setMedium(Medium medium) {
                    this.medium = medium;
                }

                public void setHigh(High high) {
                    this.high = high;
                }

            }
            public class High {
                private String url;
                private float width;
                private float height;


                // Getter Methods

                public String getUrl() {
                    return url;
                }

                public float getWidth() {
                    return width;
                }

                public float getHeight() {
                    return height;
                }

                // Setter Methods

                public void setUrl(String url) {
                    this.url = url;
                }

                public void setWidth(float width) {
                    this.width = width;
                }

                public void setHeight(float height) {
                    this.height = height;
                }
            }
            public class Medium {
                private String url;
                private float width;
                private float height;


                // Getter Methods

                public String getUrl() {
                    return url;
                }

                public float getWidth() {
                    return width;
                }

                public float getHeight() {
                    return height;
                }

                // Setter Methods

                public void setUrl(String url) {
                    this.url = url;
                }

                public void setWidth(float width) {
                    this.width = width;
                }

                public void setHeight(float height) {
                    this.height = height;
                }
            }
            public class Default {
                private String url;
                private float width;
                private float height;


                // Getter Methods

                public String getUrl() {
                    return url;
                }

                public float getWidth() {
                    return width;
                }

                public float getHeight() {
                    return height;
                }

                // Setter Methods

                public void setUrl(String url) {
                    this.url = url;
                }

                public void setWidth(float width) {
                    this.width = width;
                }

                public void setHeight(float height) {
                    this.height = height;
                }
            }

        }
    }

}





