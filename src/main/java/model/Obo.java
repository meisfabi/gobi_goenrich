package model;

import java.util.*;

public class Obo {
    private String id;
    private String name;
    private String namespace;
    private boolean isGroundTruth = false;
    private Set<String> isA = new HashSet<>();
    private final Set<String> children = new HashSet<>();
    private final Set<String> associatedGenes = new HashSet<>();
    private final Set<String> notEnrichedGenes = new HashSet<>();

    public Obo(){

    }
    public Obo(String id, String name, String namespace, Set<String> isA){
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

    public Set<String> getIsA() {
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

    public Set<String> getChildren() {
        return children;
    }

    public Set<String> getAssociatedGenes() {
        return associatedGenes;
    }

    public Set<String> getNotEnrichedGenes() {
        return notEnrichedGenes;
    }

    public void setGroundTruth(boolean groundTruth) {
        isGroundTruth = groundTruth;
    }

    public boolean isGroundTruth() {
        return isGroundTruth;
    }
}
