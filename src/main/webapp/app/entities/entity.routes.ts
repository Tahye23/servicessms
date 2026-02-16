import { Routes } from '@angular/router';
import NavbarComponent from '../layouts/navbar/navbar.component';
import DashbordComponent from '../Dashbord/dashbord.component';
import { ImportDetailComponent } from './contact/import-detail/import-detail.component';
import { PartnershipRequestListComponent } from './partnershipRequest/list/partnershipRequestService.component';

import { UserRouteAccessService } from '../core/auth/user-route-access.service';
import { Authority } from '../config/authority.constants';
import { FeatureAccessGuard } from '../core/auth/featureAccessGuard.service';
import { AdminDataDeleteComponent } from './admin-data-delete/admin-data-delete.component';

const routes: Routes = [
  // ===== ROUTES ADMIN UNIQUEMENT =====
  {
    path: 'authority',
    data: {
      pageTitle: 'migrationApp.adminAuthority.home.title',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./admin/authority/authority.routes'),
  },

  {
    path: 'entitedetest',
    data: {
      pageTitle: 'migrationApp.entitedetest.home.title',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./entitedetest/entitedetest.routes'),
  },

  {
    path: 'requests',
    data: {
      pageTitle: 'Demandes de partenariat',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./partnershipRequest/partnershipRequest.routes'),
  },

  {
    path: 'detail-import/:bulkId',
    data: {
      pageTitle: "Détails d'import",
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    component: ImportDetailComponent,
  },

  {
    path: 'delete',
    data: {
      pageTitle: 'Suppression de données',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    component: AdminDataDeleteComponent,
  },

  // ===== ROUTES AVEC VÉRIFICATION PERMISSION + ABONNEMENT =====

  // SMS - Permission + Abonnement
  {
    path: 'send-sms',
    data: {
      pageTitle: 'Envoi SMS',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'send-sms',
      permission: 'sms',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
    loadChildren: () => import('./send-sms/send-sms.routes'),
  },

  // WhatsApp - Permission + Abonnement
  {
    path: 'send-whatsapp',
    data: {
      pageTitle: 'Envoi WhatsApp',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'whatsapp',
      permission: 'whatsapp',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
    loadChildren: () => import('./send-whatsapp/send-whatsapp.routes'),
  },

  // Templates - Permission + Abonnement
  {
    path: 'template',
    data: {
      pageTitle: 'Modèles',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'templates',
      permission: 'templates',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
    loadChildren: () => import('./template/template.routes'),
  },

  // Contacts - Permission requise
  {
    path: 'contact',
    data: {
      pageTitle: 'Contacts',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'contacts',
      permission: 'contacts',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
    loadChildren: () => import('./contact/contact.routes'),
  },

  // Groupes - Permission + Abonnement
  {
    path: 'groupe',
    data: {
      pageTitle: 'Groupes de contacts',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'groups',
      permission: 'groups',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
    loadChildren: () => import('./groupe/groupe.routes'),
  },

  // Applications/API - Permission + Abonnement
  {
    path: 'application',
    data: {
      pageTitle: 'Applications',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'api',
      permission: 'applications',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
    loadChildren: () => import('./application/application.routes'),
  },

  // Gestion utilisateurs - Permission + Abonnement
  {
    path: 'user-service',
    data: {
      pageTitle: 'Gestion des utilisateurs',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'manage-users',
      permission: 'users',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
    loadChildren: () => import('./user-service/user-service.routes'),
  },

  // Tokens API - Permission + Abonnement
  {
    path: 'user-token-api',
    data: {
      pageTitle: 'Tokens API',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'api',
      permission: 'canManageAPI',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
    loadChildren: () => import('./user-token-api/user-token-api.routes'),
  },

  // Tokens d'application - Permission requise
  {
    path: 'tokens-app',
    loadChildren: () => import('./tokens-app/tokens-app.routes'),
  },
  {
    path: 'external-api-messages',
    loadChildren: () => import('./app-external-api-messages/external-api-messages.routes'),
  },
  // API - Permission requise
  {
    path: 'sapi',
    data: {
      pageTitle: 'APIs',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      permission: 'canManageAPI',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
    loadChildren: () => import('./api/api.routes'),
  },

  // ===== ROUTES ADMIN/PARTNER UNIQUEMENT =====

  {
    path: 'company',
    data: {
      pageTitle: 'Entreprises',
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./company/company.routes'),
  },

  {
    path: 'dialogue',
    data: {
      pageTitle: 'Dialogues',
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./dialogue/dialogue.routes'),
  },

  {
    path: 'groupedecontact',
    data: {
      pageTitle: 'Groupes de contact (legacy)',
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./groupedecontact/groupedecontact.routes'),
  },

  {
    path: 'conversation',
    data: {
      pageTitle: 'Conversations',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      permission: 'conversations',
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./conversation/conversation.routes'),
  },

  {
    path: 'participant',
    data: {
      pageTitle: 'Participants',
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./participant/participant.routes'),
  },

  {
    path: 'extended-user',
    data: {
      pageTitle: 'Utilisateurs étendus',
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./extended-user/extended-user.routes'),
  },

  // ===== ROUTES ADMIN UNIQUEMENT - GESTION SYSTÈME =====

  {
    path: 'abonnement',
    data: {
      pageTitle: 'Gestion des abonnements',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./abonnement/abonnement.routes'),
  },

  {
    path: 'planabonnement',
    data: {
      pageTitle: "Plans d'abonnement",
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./planabonnement/planabonnement.routes'),
  },

  {
    path: 'service',
    data: {
      pageTitle: 'Services système',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./service/service.routes'),
  },

  {
    path: 'otp-storage',
    data: {
      pageTitle: 'Stockage OTP',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./otp-storage/otp-storage.routes'),
  },

  {
    path: 'fileextrait',
    data: {
      pageTitle: 'Extraits de fichiers',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./fileextrait/fileextrait.routes'),
  },

  {
    path: 'customer',
    data: {
      pageTitle: 'Clients',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./customer/customer.routes'),
  },

  {
    path: 'referentiel',
    data: {
      pageTitle: 'Référentiels',
      authorities: [Authority.ADMIN],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./referentiel/referentiel.routes'),
  },

  // ===== ROUTES UTILITAIRES =====

  {
    path: 'choix',
    data: {
      pageTitle: 'Choix',
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./choix/choix.routes'),
  },

  {
    path: 'question',
    data: {
      pageTitle: 'Questions',
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./question/question.routes'),
  },

  {
    path: 'reponse',
    data: {
      pageTitle: 'Réponses',
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./reponse/reponse.routes'),
  },

  /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
];

export default routes;
