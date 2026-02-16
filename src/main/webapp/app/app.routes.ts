import { Routes } from '@angular/router';

import { Authority } from 'app/config/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { errorRoute } from './layouts/error/error.route';

// Composants existants
import HomeComponent from './home/home.component';
import NavbarComponent from './layouts/navbar/navbar.component';
import LoginComponent from './login/login.component';
import { AbonnementComponent } from './entities/abonnement/list/abonnement.component';
import DashbordComponent from './Dashbord/dashbord.component';
import { ConfigComponent } from './entities/config/config.component';
import { ConversationsComponent } from './entities/conversations/conversations.component';
import { ConfigAdminComponent } from './entities/confAdmin/config-admin.component';
import { MetaSignupCallbackComponent } from './entities/config/meta-signup-callback.component';

// ===== NOUVEAUX COMPOSANTS D'ABONNEMENT =====
import { UpgradePageComponent } from './Subscription/upgradePageComponent.component';
import { FeatureAccessGuard } from './core/auth/featureAccessGuard.service';
import { SubscriptionDetailsComponent } from './Subscription/app-subscription-details.component';
import { FlowBuilderMvpComponent } from './entities/chatbot/flow-builder-mvp.component';
import { SmsDetailComponent } from './entities/send-sms/SmsDetail/sms_detail.component';

const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
    title: 'home.title',
  },
  {
    path: 'boot',
    component: FlowBuilderMvpComponent,
    title: 'home.title',
  },
  {
    path: 'meta-signup-callback',
    component: MetaSignupCallbackComponent,
  },
  { path: 'sms/:smsId', component: SmsDetailComponent },
  // ===== DASHBOARD - ACCESSIBLE À TOUS LES UTILISATEURS CONNECTÉS =====
  {
    path: 'dashbord',
    component: DashbordComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
    },
    title: 'home.title',
    canActivate: [UserRouteAccessService],
  },

  // ===== ROUTES AVEC VÉRIFICATION D'ABONNEMENT =====

  // SMS - Maintenant avec vérification d'abonnement
  {
    path: 'send-sms',
    data: {
      pageTitle: 'Envoi SMS',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'send-sms', // ⚠️ NOUVEAU : Clé pour vérification d'abonnement
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard], // ⚠️ NOUVEAU GUARD
    loadChildren: () => import('./entities/send-sms/send-sms.routes'),
  },

  // WhatsApp - Avec vérification d'abonnement
  {
    path: 'send-whatsapp',
    data: {
      pageTitle: 'Envoi WhatsApp',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'send-whatsapp', // ⚠️ NOUVEAU
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard], // ⚠️ NOUVEAU GUARD
    loadChildren: () => import('./entities/send-whatsapp/send-whatsapp.routes'),
  },

  // Templates - Avec vérification d'abonnement
  {
    path: 'template',
    data: {
      pageTitle: 'Modèles',
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'templates', // ⚠️ NOUVEAU
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard], // ⚠️ NOUVEAU GUARD
    loadChildren: () => import('./entities/template/template.routes'),
  },

  // ===== ROUTES SANS VÉRIFICATION D'ABONNEMENT (ADMIN/PARTNER SEULEMENT) =====

  {
    path: 'config',
    component: ConfigComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    title: 'home.title',
    canActivate: [UserRouteAccessService],
  },

  {
    path: 'conversations',
    component: ConversationsComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    title: 'home.title',
    canActivate: [UserRouteAccessService],
  },

  {
    path: 'config-admin',
    component: ConfigAdminComponent,
    data: {
      authorities: [Authority.ADMIN],
    },
    title: 'home.title',
    canActivate: [UserRouteAccessService],
  },

  // ===== NOUVELLES ROUTES D'ABONNEMENT =====

  {
    path: 'upgrade',
    component: UpgradePageComponent,
    title: 'home.title',
  },
  {
    path: 'billing',
    component: SubscriptionDetailsComponent,
    title: 'home.title',
  },

  {
    path: 'subscription',
    children: [
      {
        path: 'plans',
        component: HomeComponent, // Utilisez votre composant de plans existant
        title: "Plans d'abonnement",
      },
      {
        path: 'billing',
        data: {
          authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
        },
        title: 'Facturation',
        canActivate: [UserRouteAccessService],
        loadChildren: () => import('./entities/abonnement/abonnement.routes'),
      },
    ],
  },

  // ===== ROUTES EXISTANTES =====

  {
    path: '',
    component: NavbarComponent,
    outlet: 'navbar',
  },
  {
    path: 'admin',
    data: {
      authorities: [Authority.ADMIN, Authority.PARTNER],
    },
    canActivate: [UserRouteAccessService],
    loadChildren: () => import('./admin/admin.routes'),
  },
  {
    path: 'account',
    loadChildren: () => import('./account/account.route'),
  },
  {
    path: 'login',
    component: LoginComponent,
    title: 'login.title',
  },
  {
    path: '',
    loadChildren: () => import(`./entities/entity.routes`),
  },

  ...errorRoute,
];

export default routes;
