import {
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexTitleSubtitle,
  ApexDataLabels,
  ApexStroke,
  ApexGrid,
  ApexMarkers,
  ApexYAxis,
  ApexFill,
  ApexTooltip,
} from 'ng-apexcharts';

export interface DailyStats {
  day: string;
  date: string;
  count: number;
}

export type ChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  title: ApexTitleSubtitle;
};

// dashboard.model.ts

export interface TypeStatsDTO {
  type: string; // "SMS" ou "WHATSAPP"
  total: number;
  success: number;
  failed: number;
  pending: number;
  unitPrice: number; // Prix unitaire MRU
}

export interface StatsResponseDTO {
  total: number;
  typeStats: TypeStatsDTO[]; // Toutes les stats groupées par type
  abonnementSolde: number; // Solde de l'abonnement
  totalConsomme: number; // Total consommé (tous types confondus)
  soldeRestant: number;
  totalMessagesFailed: number;
  totalMessagesPending: number;
}
