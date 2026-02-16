import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IOTPStorage } from '../otp-storage.model';
import { inject, OnInit } from '@angular/core';
//import { RouterModule } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { OTPStorageService } from '../service/otp-storage.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

@Component({
  standalone: true,
  selector: 'jhi-otp-storage-detail',
  templateUrl: './otp-storage-detail.component.html',
  imports: [SharedModule, ToastModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
  providers: [MessageService],
})
export class OTPStorageDetailComponent {
  oTPStorage = input<IOTPStorage | null>(null);
  expirationMinutes: number = 5;
  id!: number; // Variable pour stocker l'ID
  constructor(
    private route: ActivatedRoute, // Pour récupérer l'ID depuis l'URL
    private messageService: MessageService,

    // Injecter le service pour l'envoi vers le backend
  ) {
    this.route.paramMap.subscribe(params => {
      this.id = Number(params.get('id')); // Convertir en nombre
      console.log('ID récupéré :', this.id);

      // Appeler la fonction pour envoyer l'ID au backend
      // this.sendSmsToBackend(this.id);
    });
  }
  protected oTPStorageService = inject(OTPStorageService);
  protected oTPStorageService1 = inject(OTPStorageService);
  previousState(): void {
    window.history.back();
  }

  // otpToBackend(id: number): void {
  //   //this.sendSms.isSent = true;
  //   this.oTPStorageService.otpenv(id).subscribe({
  //     next: response => {
  //       console.log('Réponse du backend :', response);
  //       // Vous pouvez ajouter du traitement ici en fonction de la réponse du backend
  //     },
  //     error: error => {
  //       console.error("Erreur lors de l'envoi de l'ID au backend :", error);
  //     },
  //     complete: () => {
  //       console.log("Requête d'envoi terminée.");
  //     },
  //   });
  // }

  otpToBackend(id: number): void {
    console.log('debut');
    //this.sendSms.isSent = true;
    this.oTPStorageService.otpenv(id).subscribe(
      response => {
        console.log('Réponse du backend :', response);
        // alert('SMS envoyé avec succès !'); // Remplace ceci par une notification plus jolie
        console.log('✅ Affichage de la notification de succès');
        // let var1 = JSON.parse(response);
        // const messageDetail = var1.msg ?? undefined;  // Utilisation de l'opérateur nullish coalescing (??)
        let var4 = response.message;
        console.log(var4);
        // console.log(messageDetail);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: var4, life: 3000 });

        // Vous pouvez ajouter du traitement ici en fonction de la réponse du backend
      },
      error => {
        console.error("Erreur lors de l'envoi de l'ID au backend :");

        const messageDetail2 = error.error.message; // Utilisation de l'opérateur nullish coalescing (??)

        // alert("Échec de l'envoi du SMS.");

        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: messageDetail2, life: 3000 });
      },
    );
  }

  // otpsendToBackend(id: number): void {
  //   //this.sendSms.isSent = true;
  //   this.oTPStorageService1.otpsms(id).subscribe({
  //     next: response => {
  //       console.log('Réponse du backend :', response);
  //       // Vous pouvez ajouter du traitement ici en fonction de la réponse du backend
  //     },
  //     error: error => {
  //       console.error("Erreur lors de l'envoi de l'ID au backend :", error);
  //     },
  //     complete: () => {
  //       console.log("Requête d'envoi terminée.");
  //     },
  //   });
  // }

  otpsendToBackend(id: number): void {
    console.log('debut');
    //this.sendSms.isSent = true;
    this.oTPStorageService1.otpsms(id).subscribe(
      response => {
        console.log('Réponse du backend :', response);
        // alert('SMS envoyé avec succès !'); // Remplace ceci par une notification plus jolie
        console.log('✅ Affichage de la notification de succès');
        // let var1 = JSON.parse(response);
        // const messageDetail = var1.msg ?? undefined;  // Utilisation de l'opérateur nullish coalescing (??)
        let var5 = response.message;
        console.log(var5);
        // console.log(messageDetail);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: var5, life: 3000 });

        // Vous pouvez ajouter du traitement ici en fonction de la réponse du backend
      },
      error => {
        console.error("Erreur lors de l'envoi de l'ID au backend :");

        const messageDetail3 = error.error.message; // Utilisation de l'opérateur nullish coalescing (??)

        // alert("Échec de l'envoi du SMS.");

        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: messageDetail3, life: 3000 });
      },
    );
  }
}
