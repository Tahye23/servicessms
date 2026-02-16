import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { ChoixComponent } from './list/choix.component';
import { ChoixDetailComponent } from './detail/choix-detail.component';
import { ChoixUpdateComponent } from './update/choix-update.component';
import ChoixResolve from './route/choix-routing-resolve.service';

const choixRoute: Routes = [
  {
    path: '',
    component: ChoixComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ChoixDetailComponent,
    resolve: {
      choix: ChoixResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ChoixUpdateComponent,
    resolve: {
      choix: ChoixResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ChoixUpdateComponent,
    resolve: {
      choix: ChoixResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default choixRoute;
