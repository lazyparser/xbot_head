package cn.ac.iscas.xlab.droidfacedog.entity;

import java.util.List;

/**
 * Created by lisongting on 2017/6/28.
 * 这是GsonFormat插件自动生成的JSON实体类
 */
public class CommandRecogResult {

    /**
     * 该JSON的示例：
     * sn : 1
     * ls : false
     * bg : 0
     * ed : 0
     * ws : [{"bg":0,"cw":[{"sc":0,"w":"开始"}]},{"bg":0,"cw":[{"sc":0,"w":"播放"}]}]
     */
    //sentence：表示第几句
    private int sn;
    //lastSentence:表示是否是最后一句
    private boolean ls;
    //begin，开始的标记
    private int bg;
    //end，结束标志
    private int ed;
    //words：指令词列表
    private List<WsBean> ws;

    public int getSn() {
        return sn;
    }

    public void setSn(int sn) {
        this.sn = sn;
    }

    public boolean isLs() {
        return ls;
    }

    public void setLs(boolean ls) {
        this.ls = ls;
    }

    public int getBg() {
        return bg;
    }

    public void setBg(int bg) {
        this.bg = bg;
    }

    public int getEd() {
        return ed;
    }

    public void setEd(int ed) {
        this.ed = ed;
    }

    public List<WsBean> getWs() {
        return ws;
    }

    public void setWs(List<WsBean> ws) {
        this.ws = ws;
    }

    public static class WsBean {
        /**
         * bg : 0
         * cw : [{"sc":0,"w":"开始"}]
         */
        //begin
        private int bg;
        //cw代表chineseWords
        private List<CwBean> cw;

        public int getBg() {
            return bg;
        }

        public void setBg(int bg) {
            this.bg = bg;
        }

        public List<CwBean> getCw() {
            return cw;
        }

        public void setCw(List<CwBean> cw) {
            this.cw = cw;
        }

        public static class CwBean {
            /**
             * sc : 0.0
             * w : 开始
             */
            //score
            private double sc;
            //word
            private String w;

            public double getSc() {
                return sc;
            }

            public void setSc(double sc) {
                this.sc = sc;
            }

            public String getW() {
                return w;
            }

            public void setW(String w) {
                this.w = w;
            }

            @Override
            public String toString() {
                return "CwBean{" +
                        "sc=" + sc +
                        ", w='" + w + '\'' +
                        '}';
            }
        }

        @Override
        public String toString() {
            return "WsBean{" +
                    "bg=" + bg +
                    ", cw=" + cw +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "CommandRecogResult{" +
                "sn=" + sn +
                ", ls=" + ls +
                ", bg=" + bg +
                ", ed=" + ed +
                ", ws=" + ws +
                '}';
    }
}
