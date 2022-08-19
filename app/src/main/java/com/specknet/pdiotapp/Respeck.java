package com.specknet.pdiotapp;

public class Respeck {
    float x;
    float y;
    float z;
    float gx;
    float gy;
    float gz;
    String time;
    public Respeck(){}
    public void setX(float x){
        this.x=x;
    }
    public void setY(float y){
        this.y=y;
    }
    public void setZ(float z){
        this.z=z;
    }
    public void setGX(float gx){ this.gx=gx; }
    public void setGY(float gy){
        this.gy=gy;
    }
    public void setGZ(float gz){
        this.gz=gz;
    }
    public void setTime(String time){
        this.time=time;
    }
    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }
    public float getZ() {
        return z;
    }
    public float getGX() {
        return gx;
    }
    public float getGY() {
        return gy;
    }
    public float getGZ() {
        return gz;
    }
    public String getTime(){
        return time;
    }
    public Respeck( float x, float y, float z, float gx, float gy, float gz, String time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.gx = gx;
        this.gy = gy;
        this.gz = gz;
        this.time = time;
    }
}




