import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { EntitedetestComponent } from './list/entitedetest.component';
import { EntitedetestDetailComponent } from './detail/entitedetest-detail.component';
import { EntitedetestUpdateComponent } from './update/entitedetest-update.component';
import EntitedetestResolve from './route/entitedetest-routing-resolve.service';

const entitedetestRoute: Routes = [
  {
    path: '',
    component: EntitedetestComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: EntitedetestDetailComponent,
    resolve: {
      entitedetest: EntitedetestResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: EntitedetestUpdateComponent,
    resolve: {
      entitedetest: EntitedetestResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: EntitedetestUpdateComponent,
    resolve: {
      entitedetest: EntitedetestResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default entitedetestRoute;
