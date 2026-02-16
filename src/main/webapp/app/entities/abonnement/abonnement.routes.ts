import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { AbonnementComponent } from './list/abonnement.component';
import { AbonnementDetailComponent } from './detail/abonnement-detail.component';
import { AbonnementUpdateComponent } from './update/abonnement-update.component';
import AbonnementResolve from './route/abonnement-routing-resolve.service';

const abonnementRoute: Routes = [
  {
    path: '',
    component: AbonnementComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: AbonnementDetailComponent,
    resolve: {
      abonnement: AbonnementResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: AbonnementUpdateComponent,
    resolve: {
      abonnement: AbonnementResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: AbonnementUpdateComponent,
    resolve: {
      abonnement: AbonnementResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default abonnementRoute;
