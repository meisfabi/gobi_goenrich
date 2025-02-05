package model;

import java.util.*;

public class Obo {
    private String id;
    private String name;
    private String namespace;
    private String shortestPath = "";
    private double hgPval;
    private double hgFdr;
    private double fejPval;
    private double fejFdr;
    private double ksStat;
    private double ksPval;
    private double ksFdr;
    private boolean isGroundTruth = false;
    private Set<String> isA = new HashSet<>();
    private final Set<String> children = new HashSet<>();
    private final Set<String> associatedGenes = new HashSet<>();
    private final Set<String> notEnrichedGenes = new HashSet<>();
    private final Set<String> overlappingGenes = new HashSet<>();
    private int size;

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
    public Set<String> getOverlappingGenes() {
        return overlappingGenes;
    }

    public void setGroundTruth(boolean groundTruth) {
        isGroundTruth = groundTruth;
    }

    public boolean isGroundTruth() {
        return isGroundTruth;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public double getHgPval() {
        return hgPval;
    }

    public void setHgPval(double hgPval) {
        this.hgPval = hgPval;
    }

    public void setHgFdr(double hgFdr) {
        this.hgFdr = hgFdr;
    }

    public double getHgFdr() {
        return hgFdr;
    }

    public double getFejPval() {
        return fejPval;
    }

    public void setFejPval(double fejPval) {
        this.fejPval = fejPval;
    }

    public double getFejFdr() {
        return fejFdr;
    }

    public void setFejFdr(double fejFdr) {
        this.fejFdr = fejFdr;
    }

    public double getKsPval() {
        return ksPval;
    }

    public double getKsFdr() {
        return ksFdr;
    }

    public double getKsStat() {
        return ksStat;
    }

    public void setKsFdr(double ksFdr) {
        this.ksFdr = ksFdr;
    }

    public void setKsPval(double ksPval) {
        this.ksPval = ksPval;
    }

    public void setKsStat(double ksStat) {
        this.ksStat = ksStat;
    }

    public String getShortestPath() {
        return shortestPath;
    }

    public void setShortestPath(String shortestPath) {
        this.shortestPath = shortestPath;
    }
}
