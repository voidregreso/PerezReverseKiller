package es.perez.netdiagnosis;

import java.io.Serializable;

import static es.perez.netdiagnosis.Bean.ResponseFilterRule.RULE_TYPE.STRING_REPLACE;


public class Bean {

    //Outer class is just a dummy

    public static class PageBean {
        private int index;
        private String name;
        private String count;
        private Boolean isSelected = true;


        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCount() {
            return count + ((Integer.parseInt(count)==1)?" requirement":" requirements");
        }

        public Integer getCountInt() {
            try{
                return Integer.parseInt(count);
            }catch (Exception e){
                return 0;
            }
        }

        public void setCount(String count) {
            this.count = count;
        }

        public Boolean getSelected() {
            return isSelected;
        }

        public void setSelected(Boolean selected) {
            isSelected = selected;
        }
    }

    public static class ResponseFilterRule implements Serializable{
        enum RULE_TYPE {
            STRING_REPLACE,
            BEGIN_INSERT,
            END_INSERT
        }

        private RULE_TYPE ruleType =  STRING_REPLACE;
        private String url;
        private String replaceRegex;
        private String replaceContent;
        private Boolean isEnable = true;

        public RULE_TYPE getRuleType() {
            return ruleType;
        }

        public void setRuleType(RULE_TYPE ruleType) {
            this.ruleType = ruleType;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getReplaceRegex() {
            return replaceRegex;
        }

        public void setReplaceRegex(String replaceRegex) {
            this.replaceRegex = replaceRegex;
        }

        public String getReplaceContent() {
            return replaceContent;
        }

        public void setReplaceContent(String replaceContent) {
            this.replaceContent = replaceContent;
        }

        public Boolean getEnable() {
            return isEnable;
        }

        public void setEnable(Boolean enable) {
            isEnable = enable;
        }
    }
}
