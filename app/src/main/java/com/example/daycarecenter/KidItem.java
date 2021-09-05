package com.example.daycarecenter;

public class KidItem {
    private Boolean check;
    private String name;
    private String id;
    private String rssi;

    public KidItem(Boolean check, String name, String  id) {
        this.check = check;
        this.name = name;
        this.id = id;
    }
    public boolean getCheckBox() {
        return check;
    }
    public void setCheckBox(Boolean check) {
        this.check = check;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}
