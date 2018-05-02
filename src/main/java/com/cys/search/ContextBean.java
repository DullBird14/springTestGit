package com.cys.search;

/**
 * 表示一个文章的实体
 */
public class ContextBean {
    //文章的内容
    private String content;
    //文章对应的编号
    private String id;

    public ContextBean(String content, String id) {
        this.content = content;
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
