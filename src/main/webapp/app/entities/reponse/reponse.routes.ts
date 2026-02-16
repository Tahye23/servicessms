import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { ReponseComponent } from './list/reponse.component';
import { ReponseDetailComponent } from './detail/reponse-detail.component';
import { ReponseUpdateComponent } from './update/reponse-update.component';
import ReponseResolve from './route/reponse-routing-resolve.service';

const reponseRoute: Routes = [
  {
    path: '',
    component: ReponseComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ReponseDetailComponent,
    resolve: {
      reponse: ReponseResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ReponseUpdateComponent,
    resolve: {
      reponse: ReponseResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ReponseUpdateComponent,
    resolve: {
      reponse: ReponseResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default reponseRoute;
