import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { ApiComponent } from './list/api.component';
import { ApiDetailComponent } from './detail/api-detail.component';
import { ApiUpdateComponent } from './update/api-update.component';
import ApiResolve from './route/api-routing-resolve.service';

const apiRoute: Routes = [
  {
    path: '',
    component: ApiComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ApiDetailComponent,
    resolve: {
      api: ApiResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ApiUpdateComponent,
    resolve: {
      api: ApiResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ApiUpdateComponent,
    resolve: {
      api: ApiResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default apiRoute;
