package com.study.platform.note.mapper;

/**
 * 笔记本或标签维度的笔记计数。color 只在标签分布里有值。
 */
public class NameCountRow {

    private String name;
    private String color;
    private Long total;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
