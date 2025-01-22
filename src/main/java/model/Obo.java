package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Obo {
    private String id;
    private String name;
    private String namespace;
    private List<String> isA = new ArrayList<>();

    public Obo(){

    }
    public Obo(String id, String name, String namespace, List<String> isA){
        this.id = id;
        this.name = name;
        this.namespace = namespace;
        this.isA = isA;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<String> getIsA() {
        return isA;
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setIsA(List<String> isA) {
        this.isA = isA;
    }
}
