package cn.ac.iscas.xlab.droidfacedog.entity;

/**
 * Created by lisongting on 2017/7/28.
 */

public class AIMessage {

    /**
     * 当发起AI对话时：
     * 从服务端返回的AI实体类
     * intent : {"sid":"cid6f1877c2@ch00b00cd64625040001",
     * "operation":"ANSWER","text":"你是谁","service":"chat",
     * "man_intv":"","answer":{"topic":"chat_aiui个性化_你是谁",
     * "text":"我就是我，是颜色不一样的烟火。",
     * "answerType":"qa","topicID":"2147911",
     * "question":{"ws":"你 是 谁","q":"你是谁"},
     * "type":"T","isCustomized":"","emotion":"neutral"},
     * "uuid":"atn002a8d20@un46360cd646256f2601",
     * "rc":0,"no_nlu_result":0}
     */

    private IntentBean intent;

    public IntentBean getIntent() {
        return intent;
    }

    public void setIntent(IntentBean intent) {
        this.intent = intent;
    }

    public static class IntentBean {
        /**
         * sid : cid6f1877c2@ch00b00cd64625040001
         * operation : ANSWER
         * text : 你是谁
         * service : chat
         * man_intv :
         * answer : {"topic":"chat_aiui个性化_你是谁","text":"我就是我，是颜色不一样的烟火。","answerType":"qa","topicID":"2147911","question":{"ws":"你 是 谁","q":"你是谁"},"type":"T","isCustomized":"","emotion":"neutral"}
         * uuid : atn002a8d20@un46360cd646256f2601
         * rc : 0
         * no_nlu_result : 0
         */

        private String sid;
        private String operation;
        private String text;
        private String service;
        private String man_intv;
        private AnswerBean answer;
        private String uuid;
        private int rc;
        private int no_nlu_result;

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getMan_intv() {
            return man_intv;
        }

        public void setMan_intv(String man_intv) {
            this.man_intv = man_intv;
        }

        public AnswerBean getAnswer() {
            return answer;
        }

        public void setAnswer(AnswerBean answer) {
            this.answer = answer;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public int getRc() {
            return rc;
        }

        public void setRc(int rc) {
            this.rc = rc;
        }

        public int getNo_nlu_result() {
            return no_nlu_result;
        }

        public void setNo_nlu_result(int no_nlu_result) {
            this.no_nlu_result = no_nlu_result;
        }

        public static class AnswerBean {
            /**
             * topic : chat_aiui个性化_你是谁
             * text : 我就是我，是颜色不一样的烟火。
             * answerType : qa
             * topicID : 2147911
             * question : {"ws":"你 是 谁","q":"你是谁"}
             * type : T
             * isCustomized :
             * emotion : neutral
             */

            private String topic;
            private String text;
            private String answerType;
            private String topicID;
            private QuestionBean question;
            private String type;
            private String isCustomized;
            private String emotion;

            public String getTopic() {
                return topic;
            }

            public void setTopic(String topic) {
                this.topic = topic;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }

            public String getAnswerType() {
                return answerType;
            }

            public void setAnswerType(String answerType) {
                this.answerType = answerType;
            }

            public String getTopicID() {
                return topicID;
            }

            public void setTopicID(String topicID) {
                this.topicID = topicID;
            }

            public QuestionBean getQuestion() {
                return question;
            }

            public void setQuestion(QuestionBean question) {
                this.question = question;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getIsCustomized() {
                return isCustomized;
            }

            public void setIsCustomized(String isCustomized) {
                this.isCustomized = isCustomized;
            }

            public String getEmotion() {
                return emotion;
            }

            public void setEmotion(String emotion) {
                this.emotion = emotion;
            }

            public static class QuestionBean {
                /**
                 * ws : 你 是 谁
                 * q : 你是谁
                 */

                private String ws;
                private String q;

                public String getWs() {
                    return ws;
                }

                public void setWs(String ws) {
                    this.ws = ws;
                }

                public String getQ() {
                    return q;
                }

                public void setQ(String q) {
                    this.q = q;
                }
            }
        }
    }
}
