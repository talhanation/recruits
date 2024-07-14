package com.talhanation.recruits.client.gui.group;
public class RecruitsGroup {
    public String name;
    private int count;
    private boolean disabled;
    public int id;

    public RecruitsGroup(int id, String name, boolean disabled) {
        this.id = id;
        this.name = name;
        this.disabled = disabled;
    }

    public void setCount(int x){
        count = x;
    }

    public int getCount(){
        return count;
    }

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name =name;
    }

    public void setDisabled(boolean disabled){
        this.disabled = disabled;
    }

    public boolean isDisabled(){
        return disabled;
    }

    public int getId() {
        return id;
    }

}
