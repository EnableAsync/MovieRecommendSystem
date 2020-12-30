package com.atguigu.business.model.request;

public class TopAllMoviesRequest {

    private int num;

    public TopAllMoviesRequest(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    public void setNum( int num ) {
        this.num = num;
    }

}
