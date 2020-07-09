package com.yash.ymplayer.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class PopularPlaylist implements Serializable {
    String id;
    Snippet snippet;
    ContentDetails contentDetails;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Snippet getSnippet() {
        return snippet;
    }

    public void setSnippet(Snippet snippet) {
        this.snippet = snippet;
    }

    public ContentDetails getContentDetails() {
        return contentDetails;
    }

    public void setContentDetails(ContentDetails contentDetails) {
        this.contentDetails = contentDetails;
    }

   public static class Snippet{
        Localized localized;
        Thumbnails thumbnails;

       public Localized getLocalized() {
           return localized;
       }

       public void setLocalized(Localized localized) {
           this.localized = localized;
       }

       public Thumbnails getThumbnails() {
            return thumbnails;
        }

        public void setThumbnails(Thumbnails thumbnails) {
            this.thumbnails = thumbnails;
        }


        public static class Localized{
            String title;
            String description;

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }
        }

        public static class Thumbnails{
            Medium medium;
            Standard standard;

            public Medium getMedium() {
                return medium;
            }

            public void setMedium(Medium medium) {
                this.medium = medium;
            }

            public Standard getStandard() {
                return standard;
            }

            public void setStandard(Standard standard) {
                this.standard = standard;
            }

            public static class Medium{
                int width,height;
                String url;

                public int getWidth() {
                    return width;
                }

                public void setWidth(int width) {
                    this.width = width;
                }

                public int getHeight() {
                    return height;
                }

                public void setHeight(int height) {
                    this.height = height;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }
            }
            public static class Standard{
                int width,height;
                String url;

                public int getWidth() {
                    return width;
                }

                public void setWidth(int width) {
                    this.width = width;
                }

                public int getHeight() {
                    return height;
                }

                public void setHeight(int height) {
                    this.height = height;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }
            }
        }
    }
    public static class ContentDetails{
        @SerializedName("itemCount")
        String itemCount;

        public String getItemCount() {
            return itemCount;
        }

        public void setItemCount(String itemCount) {
            this.itemCount = itemCount;
        }
    }
}
//https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&fields=items(id,snippet(localized,thumbnails(medium,standard)),contentDetails)&maxResults=50&id=RDCLAK5uy_ksEjgm3H_7zOJ_RHzRjN1wY-_FFcs7aAU,RDCLAK5uy_n9Fbdw7e6ap-98_A-8JYBmPv64v-Uaq1g,RDCLAK5uy_kmPRjHDECIcuVwnKsx2Ng7fyNgFKWNJFs,RDCLAK5uy_lBGRuQnsG37Akr1CY4SxL0VWFbPrbO4gs,RDCLAK5uy_kuo_NioExeUmw07dFf8BzQ64DFFTlgE7Q,RDCLAK5uy_mOvRWCE7v4C98UgkSVh5FTlD3osGjolas,RDCLAK5uy_nTbyVypdXPQd00z15bTWjZr7pG-26yyQ4,RDCLAK5uy_n17q7_2dwfDqWckpccDyTTkZ-g03jXuII,RDCLAK5uy_kLWIr9gv1XLlPbaDS965-Db4TrBoUTxQ8,RDCLAK5uy_lyVnWI5JnuwKJiuE-n1x-Un0mj9WlEyZw,RDCLAK5uy_lJ8xZWiZj2GCw7MArjakb6b0zfvqwldps,RDCLAK5uy_mu-BhJj3yO1OXEMzahs_aJVtNWJwAwFEE&key=AIzaSyAdfvLnL55J-5dOwiL_IDF5PydkF3r2jQA