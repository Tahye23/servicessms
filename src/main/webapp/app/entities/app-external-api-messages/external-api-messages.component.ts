// external-api-messages.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExternalApiMessagesService, ExternalApiStats, ExternalApiMessage } from './external-api-messages.service';

@Component({
  selector: 'app-external-api-messages',
  templateUrl: './external-api-messages.component.html',
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class ExternalApiMessagesComponent implements OnInit {
  messages: ExternalApiMessage[] = [];
  stats: ExternalApiStats | null = null;

  // Filtres
  selectedStatus: string = '';
  selectedType: string = '';
  searchTerm: string = '';

  // Pagination
  currentPage: number = 0;
  pageSize: number = 20;
  totalElements: number = 0;
  totalPages: number = 0;

  loading: boolean = false;

  // Modal de détails
  showDetailsModal: boolean = false;
  selectedMessage: ExternalApiMessage | null = null;

  constructor(private service: ExternalApiMessagesService) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadMessages();
  }

  loadStats(): void {
    this.service.getStats().subscribe(
      stats => {
        this.stats = stats;
        console.log('Stats chargées:', stats);
      },
      error => console.error('Erreur chargement stats:', error),
    );
  }

  loadMessages(): void {
    this.loading = true;

    this.service
      .getMessages(
        this.currentPage,
        this.pageSize,
        this.selectedStatus || undefined,
        this.selectedType || undefined,
        this.searchTerm || undefined,
      )
      .subscribe(
        response => {
          this.messages = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.loading = false;
          console.log('Messages chargés:', response);
        },
        error => {
          console.error('Erreur chargement messages:', error);
          this.loading = false;
        },
      );
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadMessages();
  }

  resetFilters(): void {
    this.selectedStatus = '';
    this.selectedType = '';
    this.searchTerm = '';
    this.currentPage = 0;
    this.loadMessages();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadMessages();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadMessages();
    }
  }

  showDetails(message: ExternalApiMessage): void {
    this.selectedMessage = message;
    this.showDetailsModal = true;
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedMessage = null;
  }

  getMetadata(message: ExternalApiMessage): any {
    try {
      if (!message.last_error) return {};

      // Si c'est déjà un objet JSON
      if (message.last_error.startsWith('{')) {
        return JSON.parse(message.last_error);
      }

      return {};
    } catch (e) {
      console.error('Erreur parsing metadata:', e);
      return {};
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'SENT':
        return 'text-green-700 bg-green-100 border border-green-200';
      case 'FAILED':
        return 'text-red-700 bg-red-100 border border-red-200';
      default:
        return 'text-gray-700 bg-gray-100 border border-gray-200';
    }
  }

  getTypeColor(type: string): string {
    if (type === 'SMS') {
      return 'text-blue-700 bg-blue-100 border border-blue-200';
    } else {
      return 'text-green-700 bg-green-100 border border-green-200';
    }
  }
}
