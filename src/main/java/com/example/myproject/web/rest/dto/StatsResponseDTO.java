package com.example.myproject.web.rest.dto;

import java.util.List;

public class StatsResponseDTO {

    private List<TypeStatsDTO> typeStats;
    private double abonnementSolde;
    private double totalConsomme;
    private double soldeRestant;
    private double total;
    private double totalMessagesFailed;
    private double totalMessagesPending;

    public StatsResponseDTO(
        List<TypeStatsDTO> typeStats,
        double abonnementSolde,
        double totalConsomme,
        double soldeRestant,
        double total,
        double totalMessagesFailed,
        double totalMessagesPending
    ) {
        this.typeStats = typeStats;
        this.abonnementSolde = abonnementSolde;
        this.totalConsomme = totalConsomme;
        this.soldeRestant = soldeRestant;
        this.total = total;
        this.totalMessagesFailed = totalMessagesFailed;
        this.totalMessagesPending = totalMessagesPending;
    }

    public List<TypeStatsDTO> getTypeStats() {
        return typeStats;
    }

    public void setTypeStats(List<TypeStatsDTO> typeStats) {
        this.typeStats = typeStats;
    }

    public double getAbonnementSolde() {
        return abonnementSolde;
    }

    public void setAbonnementSolde(double abonnementSolde) {
        this.abonnementSolde = abonnementSolde;
    }

    public double getTotalConsomme() {
        return totalConsomme;
    }

    public void setTotalConsomme(double totalConsomme) {
        this.totalConsomme = totalConsomme;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getSoldeRestant() {
        return soldeRestant;
    }

    public void setSoldeRestant(double soldeRestant) {
        this.soldeRestant = soldeRestant;
    }

    public double getTotalMessagesFailed() {
        return totalMessagesFailed;
    }

    public void setTotalMessagesFailed(double totalMessagesFailed) {
        this.totalMessagesFailed = totalMessagesFailed;
    }

    public double getTotalMessagesPending() {
        return totalMessagesPending;
    }

    public void setTotalMessagesPending(double totalMessagesPending) {
        this.totalMessagesPending = totalMessagesPending;
    }
}
