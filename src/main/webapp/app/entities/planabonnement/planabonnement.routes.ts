import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { PlanabonnementComponent } from './list/planabonnement.component';
import { PlanabonnementDetailComponent } from './detail/planabonnement-detail.component';
import { PlanabonnementUpdateComponent } from './update/planabonnement-update.component';
import PlanabonnementResolve from './route/planabonnement-routing-resolve.service';

const planabonnementRoute: Routes = [
  {
    path: '',
    component: PlanabonnementComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: PlanabonnementDetailComponent,
    resolve: {
      planabonnement: PlanabonnementResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: PlanabonnementUpdateComponent,
    resolve: {
      planabonnement: PlanabonnementResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: PlanabonnementUpdateComponent,
    resolve: {
      planabonnement: PlanabonnementResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default planabonnementRoute;
