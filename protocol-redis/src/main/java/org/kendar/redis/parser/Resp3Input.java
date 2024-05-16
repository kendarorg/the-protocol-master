package org.kendar.redis.parser;

public class Resp3Input {
    private String data = null;
    private int index = 0;


    public static Resp3Input of(final String data){
        var res = new Resp3Input();
        res.setData(data);
        res.setIndex(0);
        return res;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public char charAt(int index) {
        return data.charAt(index+this.index);
    }

    public char charAtAndIncrement() {
        this.index++;
        return data.charAt(this.index-1);
    }

    public boolean hasNext(){
        return index<data.length();
    }

    public void incrementIndex() {
        this.index++;
    }

    public int length() {
        return data.length()-index;
    }

    public String substring(int length) {
        var result = data.substring(index, index+length);
        index+=length;
        return result;
    }

    public String getPreString(){
        return data.substring(0,index);
    }
}
